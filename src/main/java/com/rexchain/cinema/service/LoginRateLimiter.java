package com.rexchain.cinema.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/** In-process, bounded login throttling shared by the web and API login routes. */
@Component
public class LoginRateLimiter {
    private final int maxIdentityFailures;
    private final int maxIpFailures;
    private final Duration window;
    private final int maxTrackedKeys;
    private final Clock clock;
    private final byte[] processKey = new byte[32];
    private final Map<String, State> identityStates = new LinkedHashMap<>(16, 0.75f, true);
    private final Map<String, State> ipStates = new LinkedHashMap<>(16, 0.75f, true);

    @Autowired
    public LoginRateLimiter(
            @Value("${app.auth.login.max-failed-attempts:5}") int maxIdentityFailures,
            @Value("${app.auth.login.max-ip-failed-attempts:100}") int maxIpFailures,
            @Value("${app.auth.login.window-seconds:900}") long windowSeconds,
            @Value("${app.auth.login.max-tracked-keys:10000}") int maxTrackedKeys) {
        this(maxIdentityFailures, maxIpFailures, windowSeconds, maxTrackedKeys, Clock.systemUTC());
    }

    LoginRateLimiter(int maxIdentityFailures, int maxIpFailures, long windowSeconds,
                     int maxTrackedKeys, Clock clock) {
        if (maxIdentityFailures < 1 || maxIpFailures < 1 || windowSeconds < 1 || maxTrackedKeys < 1) {
            throw new IllegalArgumentException("Login rate-limit settings must be positive");
        }
        this.maxIdentityFailures = maxIdentityFailures;
        this.maxIpFailures = maxIpFailures;
        this.window = Duration.ofSeconds(windowSeconds);
        this.maxTrackedKeys = maxTrackedKeys;
        this.clock = clock;
        new SecureRandom().nextBytes(processKey);
    }

    public synchronized Attempt acquire(String identifier, String clientIp) {
        IpReservation reservation = begin(clientIp);
        try {
            return attach(identifier, reservation);
        } catch (RuntimeException failure) {
            cancelled(reservation);
            throw failure;
        }
    }

    /** Reserves the IP budget before any account lookup or password hashing. */
    public synchronized IpReservation begin(String clientIp) {
        Instant now = clock.instant();
        String normalizedIp = normalizeIp(clientIp);
        String ipKey = digest(normalizedIp);
        pruneExpired(ipStates, now);
        State ip = ipStates.get(ipKey);
        if (ip == null) {
            makeRoom(ipStates);
            ip = new State(now);
            ipStates.put(ipKey, ip);
        }
        refresh(ip, now);
        rejectIfLimited(ip, maxIpFailures, now);
        ip.inFlight++;
        return new IpReservation(ipKey, ip);
    }

    /** Adds the identity budget after lookup, while reusing the already reserved IP slot. */
    public synchronized Attempt attach(String identifier, IpReservation reservation) {
        if (reservation == null || reservation.finished || reservation.attached) {
            throw new IllegalStateException("Login IP reservation is not available");
        }
        Instant now = clock.instant();
        // Resolved accounts must share one failure budget across aliases and source IPs.
        // The separate IP bucket below still limits password spraying from one address.
        String identityKey = digest(normalizeIdentifier(identifier));
        pruneExpired(identityStates, now);
        State identity = identityStates.get(identityKey);
        if (identity == null) {
            makeRoom(identityStates);
            identity = new State(now);
            identityStates.put(identityKey, identity);
        }
        refresh(identity, now);
        rejectIfLimited(identity, maxIdentityFailures, now);
        identity.inFlight++;
        reservation.attached = true;
        return new Attempt(identity, reservation);
    }

    public synchronized void failed(Attempt attempt) {
        if (!finish(attempt)) return;
        Instant now = clock.instant();
        recordFailure(attempt.identity, maxIdentityFailures, now);
        recordFailure(attempt.ipReservation.state, maxIpFailures, now);
    }

    public synchronized void succeeded(Attempt attempt) {
        if (!finish(attempt)) return;
        reset(attempt.identity, clock.instant());
        // Keep the IP aggregate counter: a successful account must not erase failures
        // from other accounts sharing the same address.
    }

    public synchronized void cancelled(Attempt attempt) {
        finish(attempt);
    }

    public synchronized void cancelled(IpReservation reservation) {
        release(reservation);
    }

    private boolean finish(Attempt attempt) {
        if (attempt == null || attempt.finished) return false;
        attempt.finished = true;
        if (attempt.identity.inFlight > 0) attempt.identity.inFlight--;
        release(attempt.ipReservation);
        return true;
    }

    private void release(IpReservation reservation) {
        if (reservation == null || reservation.finished) return;
        reservation.finished = true;
        if (reservation.state.inFlight > 0) reservation.state.inFlight--;
    }

    private void rejectIfLimited(State state, int maximum, Instant now) {
        if (state.lockedUntil != null && now.isBefore(state.lockedUntil)) {
            throw new LoginRateLimitException(secondsUntil(now, state.lockedUntil));
        }
        if (state.failures + state.inFlight >= maximum) {
            Instant retry = state.lockedUntil != null ? state.lockedUntil : now.plusSeconds(1);
            throw new LoginRateLimitException(secondsUntil(now, retry));
        }
    }

    private void recordFailure(State state, int maximum, Instant now) {
        if (state.failures == 0) state.windowStartedAt = now;
        state.failures++;
        if (state.failures >= maximum) state.lockedUntil = now.plus(window);
    }

    private void refresh(State state, Instant now) {
        if (state.lockedUntil != null && !now.isBefore(state.lockedUntil)) {
            reset(state, now);
        } else if (state.lockedUntil == null && state.windowStartedAt != null
                && !now.isBefore(state.windowStartedAt.plus(window))) {
            reset(state, now);
        }
    }

    private void reset(State state, Instant now) {
        state.failures = 0;
        state.windowStartedAt = now;
        state.lockedUntil = null;
    }

    private void pruneExpired(Map<String, State> states, Instant now) {
        Iterator<Map.Entry<String, State>> iterator = states.entrySet().iterator();
        while (iterator.hasNext()) {
            State state = iterator.next().getValue();
            if (state.inFlight != 0) continue;
            boolean expired = state.lockedUntil != null
                    ? !now.isBefore(state.lockedUntil)
                    : state.windowStartedAt == null || !now.isBefore(state.windowStartedAt.plus(window));
            if (expired) iterator.remove();
        }
    }

    private void makeRoom(Map<String, State> states) {
        if (states.size() < maxTrackedKeys) return;
        Iterator<Map.Entry<String, State>> iterator = states.entrySet().iterator();
        while (iterator.hasNext()) {
            State candidate = iterator.next().getValue();
            // Never evict a live lockout: doing so would let a key-flood clear it early.
            // Expired locks have already been removed by pruneExpired().
            if (candidate.inFlight == 0 && candidate.lockedUntil == null) {
                iterator.remove();
                return;
            }
        }
        throw new LoginRateLimitException(60);
    }

    private long secondsUntil(Instant now, Instant target) {
        Duration remaining = Duration.between(now, target);
        long seconds = remaining.toSeconds();
        return Math.max(1, seconds + (remaining.minusSeconds(seconds).isZero() ? 0 : 1));
    }

    private String normalizeIdentifier(String identifier) {
        return identifier == null ? "" : identifier.trim().toLowerCase(java.util.Locale.ROOT);
    }

    private String normalizeIp(String clientIp) {
        return clientIp == null || clientIp.isBlank() ? "unknown" : clientIp.trim();
    }

    private String digest(String value) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(processKey, "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException impossible) {
            throw new IllegalStateException("HmacSHA256 is unavailable", impossible);
        }
    }

    public static final class Attempt {
        private final State identity;
        private final IpReservation ipReservation;
        private boolean finished;

        private Attempt(State identity, IpReservation ipReservation) {
            this.identity = identity;
            this.ipReservation = ipReservation;
        }
    }

    public static final class IpReservation {
        private final String ipKey;
        private final State state;
        private boolean attached;
        private boolean finished;

        private IpReservation(String ipKey, State state) {
            this.ipKey = ipKey;
            this.state = state;
        }
    }

    private static final class State {
        private int failures;
        private int inFlight;
        private Instant windowStartedAt;
        private Instant lockedUntil;

        private State(Instant now) {
            this.windowStartedAt = now;
        }
    }

    synchronized int trackedIdentityKeys() { return identityStates.size(); }
}

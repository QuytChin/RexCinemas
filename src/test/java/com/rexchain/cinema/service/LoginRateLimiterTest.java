package com.rexchain.cinema.service;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LoginRateLimiterTest {
    private final AdjustableClock clock = new AdjustableClock(Instant.parse("2026-01-01T00:00:00Z"));

    @Test
    void fifthFailureLocksIdentityIpPairAndAutomaticallyUnlocksAfterWindow() {
        LoginRateLimiter limiter = limiter(5, 100, 900, 100);
        fail(limiter, "person@example.invalid", "192.0.2.10", 5);

        assertThatThrownBy(() -> limiter.acquire("person@example.invalid", "192.0.2.10"))
                .isInstanceOf(LoginRateLimitException.class)
                .satisfies(error -> assertThat(((LoginRateLimitException) error).getRetryAfterSeconds()).isEqualTo(900));

        clock.advance(Duration.ofMinutes(15));
        LoginRateLimiter.Attempt retry = limiter.acquire("person@example.invalid", "192.0.2.10");
        limiter.cancelled(retry);
    }

    @Test
    void identityFailureBudgetIsSharedAcrossIps() {
        LoginRateLimiter limiter = limiter(5, 100, 900, 100);
        fail(limiter, "person@example.invalid", "192.0.2.10", 3);
        fail(limiter, "person@example.invalid", "192.0.2.11", 2);

        assertThatThrownBy(() -> limiter.acquire("person@example.invalid", "192.0.2.12"))
                .isInstanceOf(LoginRateLimitException.class)
                .satisfies(error -> assertThat(((LoginRateLimitException) error).getRetryAfterSeconds()).isEqualTo(900));
    }

    @Test
    void successfulLoginResetsIdentityFailuresButNotSharedIpFailures() {
        LoginRateLimiter limiter = limiter(5, 20, 900, 100);
        fail(limiter, "person@example.invalid", "192.0.2.10", 4);
        LoginRateLimiter.Attempt success = limiter.acquire("person@example.invalid", "192.0.2.10");
        limiter.succeeded(success);

        fail(limiter, "person@example.invalid", "192.0.2.10", 5);
        assertThatThrownBy(() -> limiter.acquire("person@example.invalid", "192.0.2.10"))
                .isInstanceOf(LoginRateLimitException.class);

        LoginRateLimiter ipLimiter = limiter(5, 3, 900, 100);
        fail(ipLimiter, "one@example.invalid", "192.0.2.30", 2);
        ipLimiter.succeeded(ipLimiter.acquire("one@example.invalid", "192.0.2.30"));
        fail(ipLimiter, "two@example.invalid", "192.0.2.30", 1);
        assertThatThrownBy(() -> ipLimiter.acquire("three@example.invalid", "192.0.2.30"))
                .isInstanceOf(LoginRateLimitException.class);
    }

    @Test
    void concurrentRequestsCannotReserveMoreThanTheFailureBudget() throws Exception {
        LoginRateLimiter limiter = limiter(5, 100, 900, 100);
        int workers = 24;
        ExecutorService executor = Executors.newFixedThreadPool(workers);
        CountDownLatch ready = new CountDownLatch(workers);
        CountDownLatch start = new CountDownLatch(1);
        try {
            @SuppressWarnings("unchecked")
            Future<Boolean>[] results = new Future[workers];
            for (int i = 0; i < workers; i++) {
                results[i] = executor.submit(() -> {
                    ready.countDown();
                    start.await();
                    try {
                        LoginRateLimiter.Attempt attempt = limiter.acquire("race@example.invalid", "192.0.2.12");
                        limiter.failed(attempt);
                        return true;
                    } catch (LoginRateLimitException limited) {
                        return false;
                    }
                });
            }
            ready.await();
            start.countDown();
            int accepted = 0;
            for (Future<Boolean> result : results) if (result.get()) accepted++;
            assertThat(accepted).isEqualTo(5);
            assertThatThrownBy(() -> limiter.acquire("race@example.invalid", "192.0.2.12"))
                    .isInstanceOf(LoginRateLimitException.class);
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void concurrentRequestsFromDifferentIpsShareTheSameIdentityBudget() throws Exception {
        LoginRateLimiter limiter = limiter(5, 100, 900, 100);
        int workers = 24;
        ExecutorService executor = Executors.newFixedThreadPool(workers);
        CountDownLatch ready = new CountDownLatch(workers);
        CountDownLatch start = new CountDownLatch(1);
        try {
            @SuppressWarnings("unchecked")
            Future<Boolean>[] results = new Future[workers];
            for (int i = 0; i < workers; i++) {
                int worker = i;
                results[i] = executor.submit(() -> {
                    ready.countDown();
                    start.await();
                    try {
                        LoginRateLimiter.Attempt attempt = limiter.acquire(
                                "distributed@example.invalid", "192.0.2." + (100 + worker));
                        limiter.failed(attempt);
                        return true;
                    } catch (LoginRateLimitException limited) {
                        return false;
                    }
                });
            }
            ready.await();
            start.countDown();
            int accepted = 0;
            for (Future<Boolean> result : results) if (result.get()) accepted++;
            assertThat(accepted).isEqualTo(5);
            assertThatThrownBy(() -> limiter.acquire("distributed@example.invalid", "192.0.2.250"))
                    .isInstanceOf(LoginRateLimitException.class);
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void ipBucketLimitsSprayAcrossDifferentIdentifiers() {
        LoginRateLimiter limiter = limiter(5, 3, 900, 100);
        fail(limiter, "one@example.invalid", "192.0.2.13", 1);
        fail(limiter, "two@example.invalid", "192.0.2.13", 1);
        fail(limiter, "three@example.invalid", "192.0.2.13", 1);

        assertThatThrownBy(() -> limiter.acquire("four@example.invalid", "192.0.2.13"))
                .isInstanceOf(LoginRateLimitException.class);
        LoginRateLimiter.Attempt otherIp = limiter.acquire("four@example.invalid", "192.0.2.14");
        limiter.cancelled(otherIp);
    }

    @Test
    void expiredEntriesAreCleanedAndTrackingCapacityIsBoundedWithoutGlobalLockout() {
        LoginRateLimiter limiter = limiter(2, 10, 60, 1);
        LoginRateLimiter.Attempt first = limiter.acquire("one@example.invalid", "192.0.2.15");
        limiter.cancelled(first);
        LoginRateLimiter.Attempt second = limiter.acquire("two@example.invalid", "192.0.2.15");
        limiter.cancelled(second);
        assertThat(limiter.trackedIdentityKeys()).isEqualTo(1);

        clock.advance(Duration.ofMinutes(1));
        LoginRateLimiter.Attempt afterExpiry = limiter.acquire("two@example.invalid", "192.0.2.15");
        limiter.cancelled(afterExpiry);
        assertThat(limiter.trackedIdentityKeys()).isLessThanOrEqualTo(1);
    }

    @Test
    void capacityPressureCannotEvictAnActiveLockout() {
        LoginRateLimiter limiter = limiter(1, 10, 900, 1);
        limiter.failed(limiter.acquire("locked@example.invalid", "192.0.2.40"));

        assertThatThrownBy(() -> limiter.acquire("new@example.invalid", "192.0.2.41"))
                .isInstanceOf(LoginRateLimitException.class);
        assertThatThrownBy(() -> limiter.acquire("locked@example.invalid", "192.0.2.42"))
                .isInstanceOf(LoginRateLimitException.class)
                .satisfies(error -> assertThat(((LoginRateLimitException) error).getRetryAfterSeconds()).isEqualTo(900));
    }

    private LoginRateLimiter limiter(int identityLimit, int ipLimit, long windowSeconds, int maxKeys) {
        return new LoginRateLimiter(identityLimit, ipLimit, windowSeconds, maxKeys, clock);
    }

    private void fail(LoginRateLimiter limiter, String identifier, String ip, int count) {
        for (int i = 0; i < count; i++) limiter.failed(limiter.acquire(identifier, ip));
    }

    private static final class AdjustableClock extends Clock {
        private final AtomicReference<Instant> instant;

        private AdjustableClock(Instant initial) {
            this.instant = new AtomicReference<>(initial);
        }

        private void advance(Duration duration) {
            instant.updateAndGet(current -> current.plus(duration));
        }

        @Override public ZoneId getZone() { return ZoneId.of("UTC"); }
        @Override public Clock withZone(ZoneId zone) { return this; }
        @Override public Instant instant() { return instant.get(); }
    }
}

package com.rexchain.cinema.service;

import org.springframework.stereotype.Component;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class PasswordChangeRateLimiter {
    private static final int MAX_ATTEMPTS = 5; private static final long WINDOW_SECONDS = 900;
    private final ConcurrentHashMap<Long, Deque<Instant>> failures = new ConcurrentHashMap<>();
    public void requireAllowed(Long userId) { prune(userId); if (failures.getOrDefault(userId, new ArrayDeque<>()).size() >= MAX_ATTEMPTS) throw new IllegalArgumentException("Bạn đã thử đổi mật khẩu quá nhiều lần. Vui lòng thử lại sau 15 phút"); }
    public void fail(Long userId) { Deque<Instant> attempts = failures.computeIfAbsent(userId, key -> new ArrayDeque<>()); synchronized (attempts) { prune(attempts); attempts.addLast(Instant.now()); } }
    public void succeed(Long userId) { failures.remove(userId); }
    private void prune(Long userId) { Deque<Instant> attempts = failures.get(userId); if (attempts != null) synchronized (attempts) { prune(attempts); if (attempts.isEmpty()) failures.remove(userId, attempts); } }
    private void prune(Deque<Instant> attempts) { Instant cutoff = Instant.now().minusSeconds(WINDOW_SECONDS); while (!attempts.isEmpty() && attempts.peekFirst().isBefore(cutoff)) attempts.removeFirst(); }
}

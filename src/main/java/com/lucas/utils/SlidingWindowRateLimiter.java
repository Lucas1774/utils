package com.lucas.utils;

import jakarta.annotation.Nonnull;

import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Deque;

/**
 * A thread-safe sliding window rate limiter.
 * <p>Limits the number of allowed requests within a given time window using
 * a sliding window algorithm.
 * {@link SlidingWindowRateLimiter#acquirePermission()} blocks callers (does not reject them)
 * <p>Optionally supports a timeout, specifying how long a caller should wait for
 * permission to acquire before giving up.
 * If none is provided, caller will be blocked indefinitely.
 */
@SuppressWarnings("unused")
public class SlidingWindowRateLimiter {

    private static final String NON_POSITIVE_TIMEOUT = "timeout param should be positive.";
    private static final String NON_POSITIVE_WINDOW = "window param should be positive.";
    private static final String NON_POSITIVE_MAX_REQUESTS = "maxRequest param should be positive.";
    private final int maxRequests;
    private final long windowNanos;
    private final Deque<Long> timestamps = new ArrayDeque<>();
    private final long timeout;

    public SlidingWindowRateLimiter(int maxRequests, @Nonnull Duration window) {
        if (0 >= maxRequests) {
            throw new IllegalArgumentException(NON_POSITIVE_MAX_REQUESTS);
        }
        if (!window.isPositive()) {
            throw new IllegalArgumentException(NON_POSITIVE_WINDOW);
        }
        this.maxRequests = maxRequests;
        windowNanos = window.toNanos();
        timeout = 0;
    }

    public SlidingWindowRateLimiter(int maxRequests, @Nonnull Duration window, @Nonnull Duration timeout) {
        if (0 >= maxRequests) {
            throw new IllegalArgumentException(NON_POSITIVE_MAX_REQUESTS);
        }
        if (!window.isPositive()) {
            throw new IllegalArgumentException(NON_POSITIVE_WINDOW);
        }
        if (!timeout.isPositive()) {
            throw new IllegalArgumentException(NON_POSITIVE_TIMEOUT);
        }
        this.maxRequests = maxRequests;
        windowNanos = window.toNanos();
        this.timeout = timeout.toNanos();
    }


    /**
     * Attempts to acquire permission, respecting rate limits.
     * <p>If the current number of requests within the window is below the limit,
     * permission is granted immediately. Otherwise, this method waits until
     * a slot becomes available or the configured timeout elapses.
     *
     * @return {@code true} if permission was acquired; {@code false} if the timeout expired
     */
    public synchronized boolean acquirePermission() {
        long deadline = 0 < timeout ? System.nanoTime() + timeout : Long.MAX_VALUE;
        while (true) {
            long now = System.nanoTime();
            if (now >= deadline) {
                return false;
            }
            boolean removed = false;
            while (!timestamps.isEmpty() && timestamps.peekFirst() <= now - windowNanos) {
                timestamps.pollFirst();
                removed = true;
            }
            if (removed) {
                notifyAll();
            }
            if (timestamps.size() < maxRequests) {
                timestamps.addLast(now);
                return true;
            }
            Long oldest = timestamps.peekFirst();
            if (null == oldest) {
                continue;
            }
            long waitTime = (oldest + windowNanos) - now;
            if (0 < waitTime) {
                try {
                    wait(waitTime / 1_000_000L, (int) (waitTime % 1_000_000L));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return false;
                }
            }
        }
    }
}

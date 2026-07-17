package com.lucas.utils.ratelimiter;

import jakarta.annotation.Nonnull;

import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Deque;

public abstract class AbstractSlidingWindowRateLimiter implements ISlidingWindowRateLimiter {

    private static final String NON_POSITIVE_MAX_REQUESTS = "maxRequest param should be positive.";
    private static final String NON_POSITIVE_WINDOW = "window param should be positive.";
    protected static final String RATE_LIMIT_TIMEOUT =
            "Could not acquire rate limit permit within the configured timeout.";
    protected final Deque<Long> timestamps = new ArrayDeque<>();
    protected final int maxRequests;
    protected final long windowNanos;
    protected final long timeout;

    protected AbstractSlidingWindowRateLimiter(int maxRequests, @Nonnull Duration window) {
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

    protected AbstractSlidingWindowRateLimiter(int maxRequests, @Nonnull Duration window, @Nonnull Duration timeout) {
        if (0 >= maxRequests) {
            throw new IllegalArgumentException(NON_POSITIVE_MAX_REQUESTS);
        }
        if (!window.isPositive()) {
            throw new IllegalArgumentException(NON_POSITIVE_WINDOW);
        }
        if (!timeout.isPositive()) {
            throw new IllegalArgumentException("timeout param should be positive.");
        }
        this.maxRequests = maxRequests;
        windowNanos = window.toNanos();
        this.timeout = timeout.toNanos();
    }
}

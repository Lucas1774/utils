package com.lucas.utils.ratelimiter;

import com.lucas.utils.ratelimiter.exception.RateLimitTimeoutException;
import jakarta.annotation.Nonnull;

import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.Callable;

import static com.lucas.utils.ratelimiter.SlidingWindowRateLimiter.*;

/**
 * A thread-safe sliding window rate limiter with in-flight request tracking.
 * <p>Limits the number of requests that are currently in flight or have recently completed
 * within a given time window. For permission refilling purposes,
 * time starts ticking once a previous permission-taker releases it, and not when it takes it
 * {@link #call(Callable)} blocks callers until a slot becomes available
 * or the optional timeout expires.
 * <p>Optionally supports a timeout, specifying how long a caller should wait for
 * permission to acquire before giving up.
 * If none is provided, caller will be blocked indefinitely.
 */
@SuppressWarnings("unused")
public final class CompletionSlidingWindowRateLimiter {

    private static final String RATE_LIMIT_TIMEOUT = "Could not acquire rate limit permit within the configured timeout.";
    private static final String RATE_LIMIT_INTERRUPTED = "Rate limit permit acquisition was interrupted.";

    private final Deque<Long> timestamps = new ArrayDeque<>();
    private final int maxRequests;
    private final long windowNanos;
    private final long timeout;
    private int inFlight = 0;

    public CompletionSlidingWindowRateLimiter(int maxRequests, @Nonnull Duration window) {
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

    public CompletionSlidingWindowRateLimiter(int maxRequests, @Nonnull Duration window, @Nonnull Duration timeout) {
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
     * Attempts to acquire a permit, respecting rate limits and in-flight requests.
     * <p>If the current number of requests within the window plus in-flight requests
     * is below the limit, a permit is granted immediately. Otherwise, this method
     * waits until a slot becomes available or the configured timeout elapses.
     *
     * @return the result of the task
     * @throws RateLimitTimeoutException if the timeout expired before a permit could be acquired
     */
    public <T> T call(Callable<T> task) throws Exception {
        try (var permit = acquirePermit()) {
            return task.call();
        }
    }

    private synchronized Permit acquirePermit() throws RateLimitTimeoutException {
        long deadline = 0 < timeout ? System.nanoTime() + timeout : Long.MAX_VALUE;
        while (true) {
            long now = System.nanoTime();
            if (now >= deadline) {
                throw new RateLimitTimeoutException(RATE_LIMIT_TIMEOUT);
            }
            boolean removed = false;
            while (!timestamps.isEmpty() && timestamps.peekFirst() <= now - windowNanos) {
                timestamps.pollFirst();
                removed = true;
            }
            if (removed) {
                notifyAll();
            }
            if (inFlight + timestamps.size() < maxRequests) {
                inFlight++;
                return new Permit(this);
            }

            Long oldest = timestamps.peekFirst();
            long waitTime = null == oldest
                    ? deadline - now
                    : Math.min((oldest + windowNanos) - now, deadline - now);
            if (0 < waitTime) {
                try {
                    wait(waitTime / 1_000_000L, (int) (waitTime % 1_000_000L));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RateLimitTimeoutException(RATE_LIMIT_INTERRUPTED, e);
                }
            }
        }
    }

    private synchronized void releasePermission() {
        inFlight--;
        timestamps.addLast(System.nanoTime());
        notifyAll();
    }

    static final class Permit implements AutoCloseable {
        private final CompletionSlidingWindowRateLimiter limiter;

        private Permit(CompletionSlidingWindowRateLimiter limiter) {
            this.limiter = limiter;
        }

        @Override
        public void close() {
            limiter.releasePermission();
        }
    }
}

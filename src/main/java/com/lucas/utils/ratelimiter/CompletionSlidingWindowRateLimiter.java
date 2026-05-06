package com.lucas.utils.ratelimiter;

import com.lucas.utils.ratelimiter.exception.RateLimitTimeoutException;
import jakarta.annotation.Nonnull;

import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Optional;
import java.util.concurrent.Callable;

import static com.lucas.utils.ratelimiter.SlidingWindowRateLimiter.*;

/**
 * A thread-safe sliding window rate limiter with in-flight request tracking.
 * <p>Limits the number of requests that are currently in flight or have recently completed
 * within a given time window. For permission refilling purposes,
 * time starts ticking once a previous permission-taker releases it, and not when it takes it.
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
     * Executes a task after acquiring a permit, respecting rate limits and in-flight requests.
     * <p>If the current number of requests within the window plus in-flight requests
     * is below the limit, a permit is granted immediately. Otherwise, this method
     * waits until a slot becomes available or the configured timeout elapses.
     *
     * @param task the task to execute
     * @param <T>  the return type of the task
     * @return the result of the task
     * @throws Exception                 if the task throws an exception
     * @throws RateLimitTimeoutException if the timeout expired before a permit could be acquired
     */
    public <T> T call(@Nonnull Callable<T> task) throws Exception {
        try (var permit = acquirePermission()) {
            return task.call();
        }
    }

    /**
     * Attempts to execute a task without waiting for a permit.
     * <p>If a permit is available immediately, the task is executed and its result returned.
     * Otherwise, returns an empty Optional.
     *
     * @param task the task to execute
     * @param <T>  the return type of the task
     * @return an Optional containing the task result if executed, or empty if no permit available
     * @throws Exception if the task throws an exception
     */
    public <T> Optional<T> tryCall(@Nonnull Callable<T> task) throws Exception {
        Optional<Permit> permit = tryAcquirePermission();
        if (permit.isPresent()) {
            try (Permit p = permit.get()) {
                return Optional.of(task.call());
            }
        }
        return Optional.empty();
    }

    private synchronized Permit acquirePermission() throws RateLimitTimeoutException {
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

    private synchronized Optional<Permit> tryAcquirePermission() {
        long now = System.nanoTime();
        while (!timestamps.isEmpty() && timestamps.peekFirst() <= now - windowNanos) {
            timestamps.pollFirst();
        }
        if (inFlight + timestamps.size() < maxRequests) {
            inFlight++;
            return Optional.of(new Permit(this));
        }
        return Optional.empty();
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

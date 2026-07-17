package com.lucas.utils.ratelimiter;

import com.lucas.utils.ratelimiter.exception.RateLimitTimeoutException;
import jakarta.annotation.Nonnull;

import java.time.Duration;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Callable;

@SuppressWarnings("unused")
public final class SlidingWindowRateLimiter extends AbstractSlidingWindowRateLimiter {

    public SlidingWindowRateLimiter(int maxRequests, @Nonnull Duration window) {
        super(maxRequests, window);
    }

    public SlidingWindowRateLimiter(int maxRequests, @Nonnull Duration window, @Nonnull Duration timeout) {
        super(maxRequests, window, timeout);
    }

    private synchronized void acquireOrThrow() throws RateLimitTimeoutException, InterruptedException {
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
            if (timestamps.size() < maxRequests) {
                timestamps.addLast(now);
                return;
            }
            long waitTime =
                    Math.min(Objects.requireNonNull(timestamps.peekFirst()) + windowNanos - now, deadline - now);
            if (0 < waitTime) {
                wait(waitTime / 1_000_000L, (int) (waitTime % 1_000_000L));
            }
        }
    }

    @Override
    public <T> T call(@Nonnull Callable<T> task) throws Exception {
        acquireOrThrow();
        return task.call();
    }

    @Override
    public <T> Optional<T> tryCall(@Nonnull Callable<T> task) throws Exception {
        if (!tryAcquirePermission()) {
            return Optional.empty();
        }
        return Optional.of(task.call());
    }

    /**
     * Attempts to acquire permission, respecting rate limits.
     *
     * <p>If the current number of requests within the window is below the limit,
     * permission is granted immediately. Otherwise, this method waits until
     * a slot becomes available or the configured timeout elapses.
     *
     * @return {@code true} if permission was acquired; {@code false} if interrupted or the timeout expired.
     */
    public synchronized boolean acquirePermission() {
        try {
            acquireOrThrow();
            return true;
        } catch (RateLimitTimeoutException e) {
            return false;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    /**
     * Attempts to acquire permission without waiting.
     *
     * <p>If the current number of requests within the window is below the limit,
     * permission is granted immediately. Otherwise, returns {@code false}.
     *
     * @return {@code true} if permission was acquired; {@code false} if no permit available
     */
    public synchronized boolean tryAcquirePermission() {
        long now = System.nanoTime();
        while (!timestamps.isEmpty() && timestamps.peekFirst() <= now - windowNanos) {
            timestamps.pollFirst();
        }
        if (timestamps.size() < maxRequests) {
            timestamps.addLast(now);
            return true;
        }
        return false;
    }
}

package com.lucas.utils.ratelimiter;

import com.lucas.utils.ratelimiter.exception.RateLimitTimeoutException;
import jakarta.annotation.Nonnull;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.Callable;

@SuppressWarnings("unused")
public final class CompletionSlidingWindowRateLimiter extends AbstractSlidingWindowRateLimiter {

    private int inFlight = 0;

    public CompletionSlidingWindowRateLimiter(int maxRequests, @Nonnull Duration window) {
        super(maxRequests, window);
    }

    public CompletionSlidingWindowRateLimiter(int maxRequests, @Nonnull Duration window, @Nonnull Duration timeout) {
        super(maxRequests, window, timeout);
    }

    @Override
    public <T> T call(@Nonnull Callable<T> task) throws Exception {
        try (Permit permit = acquirePermission()) {
            return task.call();
        }
    }

    @Override
    public <T> Optional<T> tryCall(@Nonnull Callable<T> task) throws Exception {
        Optional<Permit> permit = tryAcquirePermission();
        if (permit.isPresent()) {
            try (Permit p = permit.get()) {
                return Optional.of(task.call());
            }
        }
        return Optional.empty();
    }

    private synchronized Permit acquirePermission() throws RateLimitTimeoutException, InterruptedException {
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
            long waitTime = null == oldest ? deadline - now : Math.min((oldest + windowNanos) - now, deadline - now);
            if (0 < waitTime) {
                wait(waitTime / 1_000_000L, (int) (waitTime % 1_000_000L));
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

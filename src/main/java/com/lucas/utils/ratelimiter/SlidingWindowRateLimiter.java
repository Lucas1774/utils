package com.lucas.utils.ratelimiter;

import com.lucas.utils.ratelimiter.exception.RateLimitTimeoutException;
import jakarta.annotation.Nonnull;

import java.util.Optional;
import java.util.concurrent.Callable;

/**
 * A sliding window rate limiter.
 *
 * <p>{@link #call(Callable)} waits for permission before running the task,
 * blocking the caller for as long as the implementation's policy requires.
 * {@link #tryCall(Callable)} is the non-blocking counterpart: it runs the task
 * immediately if permission is available, or returns without running it otherwise.
 */
@SuppressWarnings("unused")
public interface SlidingWindowRateLimiter {

    /**
     * Executes a task after acquiring permission, respecting rate limits.
     *
     * <p>Blocks the caller until a slot becomes available or the configured
     * timeout elapses.
     *
     * @param task the task to execute
     * @param <T>  the return type of the task
     * @return the result of the task
     * @throws Exception                 if the task throws an exception
     * @throws RateLimitTimeoutException if the timeout expired before permission
     *                                   could be acquired
     * @throws InterruptedException      if the calling thread is interrupted while
     *                                   waiting for permission to be acquired
     */
    <T> T call(@Nonnull Callable<T> task) throws Exception;

    /**
     * Attempts to execute a task without waiting for permission.
     *
     * <p>If permission is available immediately, the task is executed and its
     * result returned. Otherwise, returns an empty Optional.
     *
     * @param task the task to execute
     * @param <T>  the return type of the task
     * @return an Optional containing the task result if executed, or empty if no permission available
     * @throws Exception if the task throws an exception
     */
    <T> Optional<T> tryCall(@Nonnull Callable<T> task) throws Exception;
}

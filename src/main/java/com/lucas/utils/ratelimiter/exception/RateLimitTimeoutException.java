package com.lucas.utils.ratelimiter.exception;

import com.lucas.utils.ratelimiter.CompletionSlidingWindowRateLimiter;

/**
 * Exception thrown when a rate limit permit could not be acquired within the configured timeout.
 * <p>This exception is intended for use only by {@link CompletionSlidingWindowRateLimiter}.
 */
@SuppressWarnings("unused")
public class RateLimitTimeoutException extends Exception {

    public RateLimitTimeoutException(String message) {
        super(message);
    }

    public RateLimitTimeoutException(String message, Throwable cause) {
        super(message, cause);
    }
}

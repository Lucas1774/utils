package com.lucas.utils.exception;

import com.lucas.utils.Mapper;

/**
 * Exception thrown when a key cannot be mapped to a value.
 * <p>This exception is intended for use only by {@link Mapper} implementations.
 */
@SuppressWarnings("unused")
public class UncheckedInterruptedException extends RuntimeException {

    public UncheckedInterruptedException(Throwable cause) {
        super(cause);
    }
}

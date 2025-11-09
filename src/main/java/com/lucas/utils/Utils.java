package com.lucas.utils;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.function.Consumer;
import java.util.function.Supplier;

@SuppressWarnings("unused")
public final class Utils {

    public static final String EMPTY_STRING = "";
    public static final Runnable NOOP = () -> {
    };

    private Utils() {
    }

    /**
     * Returns the value from {@code getter} if non-null;
     * otherwise obtains a new value from {@code supplier}, sets it using {@code setter}, and returns it.
     *
     * @param getter   getter
     * @param setter   setter1
     * @param supplier supplier
     * @param <T>      value type
     * @return existing value from getter if non-null; otherwise the value produced by supplier
     */
    public static <T> @Nullable T computeIfAbsent(@Nonnull Supplier<T> getter,
                                                  @Nonnull Consumer<T> setter,
                                                  @Nonnull Supplier<T> supplier) {
        T current = getter.get();
        if (null != current) {
            return current;
        }
        T next = supplier.get();
        setter.accept(next);
        return next;
    }
}

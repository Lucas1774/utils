package com.lucas.utils;

import com.lucas.utils.exception.UncheckedInterruptedException;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.function.Consumer;
import java.util.function.Supplier;

@SuppressWarnings("unused")
public final class Interrupts {

    private Interrupts() {
    }

    /**
     * Runs a task and swallows {@link InterruptedException} after interrupting the thread
     *
     * @param task        task to run
     * @param onInterrupt on interrupt callback
     */
    public static void runOrSwallow(@Nonnull IORunnable task,
                                    @Nonnull Consumer<? super InterruptedException> onInterrupt) {
        try {
            task.run();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            onInterrupt.accept(e);
        }
    }

    /**
     * Calls a task and swallows {@link InterruptedException} after interrupting the thread
     *
     * @param task            task to call
     * @param defaultProvider default value provider if interrupted
     * @param onInterrupt     on interrupt callback
     * @param <T>             return type
     * @return value returned by task or default value if interrupted
     */
    @Nullable
    public static <T> T callOrSwallow(@Nonnull IOCallable<T> task,
                                      @Nonnull Supplier<T> defaultProvider,
                                      @Nonnull Consumer<? super InterruptedException> onInterrupt) {
        try {
            return task.call();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            onInterrupt.accept(e);
            return defaultProvider.get();
        }
    }

    /**
     * Runs a task and throws {@link RuntimeException} after interrupting the thread
     * when the task throws {@link InterruptedException}
     *
     * @param task        task to run
     * @param onInterrupt on interrupt callback
     */
    public static void runOrThrow(@Nonnull IORunnable task,
                                  @Nonnull Consumer<? super InterruptedException> onInterrupt) {
        try {
            task.run();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            onInterrupt.accept(e);
            throw new UncheckedInterruptedException(e);
        }
    }

    /**
     * Calls a task and throws {@link RuntimeException} after interrupting the thread
     * when the task throws {@link InterruptedException}
     *
     * @param task        task to call
     * @param onInterrupt on interrupt callback
     * @param <T>         return type
     * @return value returned by task
     */
    @Nullable
    public static <T> T callOrThrow(@Nonnull IOCallable<T> task,
                                    @Nonnull Consumer<? super InterruptedException> onInterrupt) {
        try {
            return task.call();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            onInterrupt.accept(e);
            throw new UncheckedInterruptedException(e);
        }
    }

    @SuppressWarnings("RedundantThrows")
    @FunctionalInterface
    public interface IORunnable {
        void run() throws InterruptedException;
    }

    @SuppressWarnings("RedundantThrows")
    @FunctionalInterface
    public interface IOCallable<T> {
        T call() throws InterruptedException;
    }
}

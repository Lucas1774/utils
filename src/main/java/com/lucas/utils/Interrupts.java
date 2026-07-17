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
    public static void runOrSwallow(@Nonnull Runnable task,
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
    public static <T> T callOrSwallow(@Nonnull Callable<T> task,
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
    public static void runOrThrow(@Nonnull Runnable task, @Nonnull Consumer<? super InterruptedException> onInterrupt) {
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
    public static <T> T callOrThrow(@Nonnull Callable<T> task,
                                    @Nonnull Consumer<? super InterruptedException> onInterrupt) {
        try {
            return task.call();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            onInterrupt.accept(e);
            throw new UncheckedInterruptedException(e);
        }
    }

    /**
     * A task that can be executed.
     */
    @SuppressWarnings("RedundantThrows")
    @FunctionalInterface
    public interface Runnable {
        /**
         * Executes the task.
         *
         * @throws InterruptedException if interrupted
         */
        void run() throws InterruptedException;
    }

    /**
     * A task that produces a result.
     */
    @SuppressWarnings("RedundantThrows")
    @FunctionalInterface
    public interface Callable<T> {
        /**
         * Executes the task.
         *
         * @return the result
         * @throws InterruptedException if interrupted
         */
        T call() throws InterruptedException;
    }
}

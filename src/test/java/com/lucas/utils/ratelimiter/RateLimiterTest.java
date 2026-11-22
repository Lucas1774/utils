package com.lucas.utils.ratelimiter;

import com.lucas.utils.ratelimiter.exception.RateLimitTimeoutException;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RateLimiterTest {

    @Test
    void defaultRateLimiter_shouldRestoreCapacityOnRefund() throws Exception {
        DefaultSlidingWindowRateLimiter limiter = new DefaultSlidingWindowRateLimiter(1, Duration.ofMinutes(1));

        Optional<String> first = limiter.tryCall(() -> "success");
        assertTrue(first.isPresent());
        assertEquals("success", first.get());

        Optional<String> second = limiter.tryCall(() -> "should fail");
        assertFalse(second.isPresent());

        limiter.refundPermission();

        Optional<String> third = limiter.tryCall(() -> "success again");
        assertTrue(third.isPresent());
        assertEquals("success again", third.get());
    }

    @Test
    void completionRateLimiter_shouldRestoreCapacityOnRefund() throws Exception {
        CompletionSlidingWindowRateLimiter limiter = new CompletionSlidingWindowRateLimiter(1, Duration.ofMinutes(1));

        Optional<String> first = limiter.tryCall(() -> "success");
        assertTrue(first.isPresent());
        assertEquals("success", first.get());

        Optional<String> second = limiter.tryCall(() -> "should fail");
        assertFalse(second.isPresent());

        limiter.refundPermission();

        Optional<String> third = limiter.tryCall(() -> "success again");
        assertTrue(third.isPresent());
        assertEquals("success again", third.get());
    }

    @Test
    void defaultRateLimiter_shouldRefundTheCallingThreadsPermit() throws Exception {
        assertRefundTargetsCallingThreadsPermit(new DefaultSlidingWindowRateLimiter(2, Duration.ofMinutes(1)));
    }

    @Test
    void completionRateLimiter_shouldRefundTheCallingThreadsPermit() throws Exception {
        assertRefundTargetsCallingThreadsPermit(new CompletionSlidingWindowRateLimiter(2, Duration.ofMinutes(1)));
    }

    @Test
    void defaultRateLimiter_shouldRefundPermitAfterCallThrows() throws Exception {
        DefaultSlidingWindowRateLimiter limiter = new DefaultSlidingWindowRateLimiter(1, Duration.ofMinutes(1));
        Exception clientException = new Exception("Service unavailable");

        assertSame(clientException, assertThrows(Exception.class, () -> limiter.call(() -> {
            throw clientException;
        })));
        limiter.refundPermission();

        assertTrue(limiter.tryCall(() -> "success").isPresent());
    }

    @Test
    void completionRateLimiter_shouldRefundPermitAfterCallThrows() throws Exception {
        CompletionSlidingWindowRateLimiter limiter = new CompletionSlidingWindowRateLimiter(1, Duration.ofMinutes(1));
        Exception clientException = new Exception("Service unavailable");

        assertSame(clientException, assertThrows(Exception.class, () -> limiter.call(() -> {
            throw clientException;
        })));
        limiter.refundPermission();

        assertTrue(limiter.tryCall(() -> "success").isPresent());
    }

    @Test
    void defaultRateLimiter_tryCallShouldNotRunTaskWhenLimitIsReached() throws Exception {
        assertTryCallDoesNotRunTaskWhenLimitIsReached(new DefaultSlidingWindowRateLimiter(1, Duration.ofMinutes(1)));
    }

    @Test
    void completionRateLimiter_tryCallShouldNotRunTaskWhenLimitIsReached() throws Exception {
        assertTryCallDoesNotRunTaskWhenLimitIsReached(new CompletionSlidingWindowRateLimiter(1, Duration.ofMinutes(1)));
    }

    @Test
    void defaultRateLimiter_callShouldWaitForWindowCapacity() throws Exception {
        assertCallWaitsForWindowCapacity(new DefaultSlidingWindowRateLimiter(1, Duration.ofMillis(100)));
    }

    @Test
    void completionRateLimiter_callShouldWaitForWindowCapacity() throws Exception {
        assertCallWaitsForWindowCapacity(new CompletionSlidingWindowRateLimiter(1, Duration.ofMillis(100)));
    }

    @Test
    void defaultRateLimiter_callShouldThrowWhenTimeoutExpires() throws Exception {
        assertCallTimesOut(new DefaultSlidingWindowRateLimiter(1, Duration.ofMinutes(1), Duration.ofMillis(50)));
    }

    @Test
    void completionRateLimiter_callShouldThrowWhenTimeoutExpires() throws Exception {
        assertCallTimesOut(new CompletionSlidingWindowRateLimiter(1, Duration.ofMinutes(1), Duration.ofMillis(50)));
    }

    @Test
    void defaultRateLimiter_shouldStartWindowAtAcquisition() throws Exception {
        assertWindowStartsAtAcquisition(new DefaultSlidingWindowRateLimiter(1, Duration.ofMillis(500)), true);
    }

    @Test
    void completionRateLimiter_shouldStartWindowAtCompletion() throws Exception {
        assertWindowStartsAtAcquisition(new CompletionSlidingWindowRateLimiter(1, Duration.ofSeconds(5)), false);
    }

    @Test
    void defaultRateLimiter_shouldPropagateTaskException() {
        assertTaskExceptionPropagated(new DefaultSlidingWindowRateLimiter(1, Duration.ofMinutes(1)));
    }

    @Test
    void completionRateLimiter_shouldPropagateTaskException() {
        assertTaskExceptionPropagated(new CompletionSlidingWindowRateLimiter(1, Duration.ofMinutes(1)));
    }

    private void assertTryCallDoesNotRunTaskWhenLimitIsReached(SlidingWindowRateLimiter limiter) throws Exception {
        AtomicBoolean taskRan = new AtomicBoolean();
        assertTrue(limiter.tryCall(() -> "first").isPresent());

        assertFalse(limiter.tryCall(() -> {
            taskRan.set(true);
            return "second";
        }).isPresent());

        assertFalse(taskRan.get());
    }

    private void assertCallWaitsForWindowCapacity(SlidingWindowRateLimiter limiter) throws Exception {
        try (ExecutorService executor = Executors.newSingleThreadExecutor()) {
            assertTrue(limiter.tryCall(() -> "first").isPresent());
            Future<String> call = executor.submit(() -> limiter.call(() -> "second"));

            assertEquals("second", call.get(2, TimeUnit.SECONDS));
        }
    }

    private void assertCallTimesOut(SlidingWindowRateLimiter limiter) throws Exception {
        assertTrue(limiter.tryCall(() -> "first").isPresent());

        assertThrows(RateLimitTimeoutException.class, () -> limiter.call(() -> "second"));
    }

    private void assertWindowStartsAtAcquisition(SlidingWindowRateLimiter limiter, boolean shouldAdmitWhileRunning)
            throws Exception {
        CountDownLatch taskStarted = new CountDownLatch(1);
        CountDownLatch finishTask = new CountDownLatch(1);
        try (ExecutorService executor = Executors.newSingleThreadExecutor()) {
            final Future<String> firstCall = executor.submit(() -> limiter.call(() -> {
                taskStarted.countDown();
                finishTask.await();
                return "first";
            }));
            assertTrue(taskStarted.await(2, TimeUnit.SECONDS));
            if (shouldAdmitWhileRunning) {
                await().atMost(Duration.ofSeconds(2)).until(() -> limiter.tryCall(() -> "second").isPresent());
            } else {
                assertFalse(limiter.tryCall(() -> "second").isPresent());
            }
            finishTask.countDown();
            assertEquals("first", firstCall.get(2, TimeUnit.SECONDS));
        } finally {
            finishTask.countDown();
        }
    }

    private void assertTaskExceptionPropagated(SlidingWindowRateLimiter limiter) {
        IOException taskException = new IOException("task failed");

        IOException thrown = assertThrows(IOException.class, () -> limiter.call(() -> {
            throw taskException;
        }));

        assertEquals(taskException, thrown);
    }

    private void assertRefundTargetsCallingThreadsPermit(AbstractSlidingWindowRateLimiter limiter) throws Exception {
        CountDownLatch firstAcquired = new CountDownLatch(1);
        CountDownLatch refundFirst = new CountDownLatch(1);
        try (ExecutorService executor = Executors.newSingleThreadExecutor()) {
            final Future<Object> firstPermit = executor.submit(() -> {
                limiter.call(() -> "first");
                final Object permit = limiter.timestamps.getLast().permit();
                firstAcquired.countDown();
                refundFirst.await();
                limiter.refundPermission();
                return permit;
            });
            assertTrue(firstAcquired.await(2, TimeUnit.SECONDS));
            assertEquals("second", limiter.call(() -> "second"));
            Object secondPermit = limiter.timestamps.getLast().permit();
            refundFirst.countDown();
            assertNotSame(firstPermit.get(2, TimeUnit.SECONDS), secondPermit);

            assertEquals(1, limiter.timestamps.size());
            assertSame(secondPermit, limiter.timestamps.getFirst().permit());
            assertTrue(limiter.tryCall(() -> "one available slot").isPresent());
            assertFalse(limiter.tryCall(() -> "second slot should still be occupied").isPresent());
        } finally {
            refundFirst.countDown();
        }
    }
}

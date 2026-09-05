package com.survisha.meghaconnect.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class VoiceTranscriptionQueueTest {
    private final ThreadPoolExecutor executor = new ThreadPoolExecutor(
            1, 1, 0, TimeUnit.MILLISECONDS, new ArrayBlockingQueue<>(5));

    @AfterEach void shutdown() { executor.shutdownNow(); }

    @Test
    void secondAppointmentQueuesImmediatelyWhileFirstIsTranscribing() throws Exception {
        VoiceTranscriptionWorker worker = mock(VoiceTranscriptionWorker.class);
        CountDownLatch firstStarted = new CountDownLatch(1);
        CountDownLatch releaseFirst = new CountDownLatch(1);
        CountDownLatch secondCompleted = new CountDownLatch(1);
        doAnswer(invocation -> {
            firstStarted.countDown();
            releaseFirst.await(5, TimeUnit.SECONDS);
            return null;
        }).when(worker).process(501L);
        doAnswer(invocation -> { secondCompleted.countDown(); return null; }).when(worker).process(502L);
        VoiceTranscriptionQueue queue = new VoiceTranscriptionQueue(executor, worker);

        assertThat(queue.submit(501L)).isTrue();
        assertThat(firstStarted.await(1, TimeUnit.SECONDS)).isTrue();

        long started = System.nanoTime();
        assertThat(queue.submit(502L)).isTrue();
        assertThat(Duration.ofNanos(System.nanoTime() - started)).isLessThan(Duration.ofMillis(250));
        verify(worker, never()).process(502L);

        releaseFirst.countDown();
        assertThat(secondCompleted.await(2, TimeUnit.SECONDS)).isTrue();
        verify(worker).process(501L);
        verify(worker).process(502L);
    }

    @Test
    void fullExecutorLeavesAdditionalJobForDatabaseRecovery() throws Exception {
        VoiceTranscriptionWorker worker = mock(VoiceTranscriptionWorker.class);
        CountDownLatch blocking = new CountDownLatch(1);
        CountDownLatch started = new CountDownLatch(1);
        doAnswer(invocation -> { started.countDown(); blocking.await(5, TimeUnit.SECONDS); return null; }).when(worker).process(anyLong());
        ThreadPoolExecutor bounded = new ThreadPoolExecutor(1, 1, 0, TimeUnit.MILLISECONDS, new ArrayBlockingQueue<>(1));
        VoiceTranscriptionQueue smallQueue = new VoiceTranscriptionQueue(bounded, worker);
        try {
            assertThat(smallQueue.submit(1L)).isTrue();
            assertThat(started.await(1, TimeUnit.SECONDS)).isTrue();
            assertThat(smallQueue.submit(2L)).isTrue();
            assertThat(smallQueue.submit(3L)).isFalse();
        } finally {
            blocking.countDown();
            bounded.shutdown();
            bounded.awaitTermination(2, TimeUnit.SECONDS);
        }
    }

    @Test
    void fiveRecordingsRemainBoundedToOneActiveTranscription() throws Exception {
        VoiceTranscriptionWorker worker = mock(VoiceTranscriptionWorker.class);
        AtomicInteger active = new AtomicInteger();
        AtomicInteger maximum = new AtomicInteger();
        CountDownLatch completed = new CountDownLatch(5);
        doAnswer(invocation -> {
            int current = active.incrementAndGet();
            maximum.accumulateAndGet(current, Math::max);
            Thread.sleep(20);
            active.decrementAndGet();
            completed.countDown();
            return null;
        }).when(worker).process(anyLong());
        VoiceTranscriptionQueue queue = new VoiceTranscriptionQueue(executor, worker);

        for (long id = 601; id <= 605; id++) assertThat(queue.submit(id)).isTrue();

        assertThat(completed.await(2, TimeUnit.SECONDS)).isTrue();
        assertThat(maximum).hasValue(1);
        for (long id = 601; id <= 605; id++) verify(worker).process(id);
    }
}

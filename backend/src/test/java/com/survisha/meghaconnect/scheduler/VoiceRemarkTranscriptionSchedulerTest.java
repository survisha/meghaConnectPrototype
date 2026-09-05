package com.survisha.meghaconnect.scheduler;

import com.survisha.meghaconnect.entity.VoiceRemark;
import com.survisha.meghaconnect.repository.VoiceRemarkRepository;
import com.survisha.meghaconnect.service.VoiceTranscriptionQueue;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.SimpleTransactionStatus;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class VoiceRemarkTranscriptionSchedulerTest {
    @Test
    void recoversStaleProcessingAndRequeuesPersistentRecords() {
        VoiceRemarkRepository repository = mock(VoiceRemarkRepository.class);
        VoiceTranscriptionQueue queue = mock(VoiceTranscriptionQueue.class);
        PlatformTransactionManager transactions = mock(PlatformTransactionManager.class);
        when(transactions.getTransaction(any(TransactionDefinition.class))).thenReturn(new SimpleTransactionStatus());
        VoiceRemark pendingA = new VoiceRemark(); pendingA.setId(701L);
        VoiceRemark pendingB = new VoiceRemark(); pendingB.setId(702L);
        when(repository.findByTranscriptionStatusInAndTranscriptionAttemptsLessThanAndUpdatedAtBeforeOrderByCreatedAtAsc(
                anyCollection(), eq(3), any(), any(Pageable.class))).thenReturn(List.of(pendingA, pendingB));
        VoiceRemarkTranscriptionScheduler scheduler = new VoiceRemarkTranscriptionScheduler(repository, queue, transactions);
        ReflectionTestUtils.setField(scheduler, "maxAttempts", 3);
        ReflectionTestUtils.setField(scheduler, "retryDelaySeconds", 60L);
        ReflectionTestUtils.setField(scheduler, "batchSize", 100);
        ReflectionTestUtils.setField(scheduler, "staleSeconds", 300L);

        scheduler.process();

        verify(repository).recoverStaleProcessing(eq(VoiceRemark.Status.PROCESSING), eq(VoiceRemark.Status.PENDING), any(), any());
        verify(queue).submit(701L);
        verify(queue).submit(702L);
    }
}

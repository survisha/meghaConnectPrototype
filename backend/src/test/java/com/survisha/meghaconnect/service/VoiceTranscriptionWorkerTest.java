package com.survisha.meghaconnect.service;

import com.survisha.meghaconnect.repository.VoiceRemarkRepository;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.SimpleTransactionStatus;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class VoiceTranscriptionWorkerTest {
    @Test
    void duplicateSubmissionDoesNotCallWhisperWhenAtomicClaimFails() {
        VoiceRemarkRepository repository = mock(VoiceRemarkRepository.class);
        VoiceRemarkStorageService storage = mock(VoiceRemarkStorageService.class);
        SpeechTranscriptionClient speech = mock(SpeechTranscriptionClient.class);
        PlatformTransactionManager transactions = mock(PlatformTransactionManager.class);
        when(transactions.getTransaction(any(TransactionDefinition.class))).thenReturn(new SimpleTransactionStatus());
        when(repository.claimForProcessing(eq(100L), any(), anyCollection(), eq(3), any())).thenReturn(0);
        VoiceTranscriptionWorker worker = new VoiceTranscriptionWorker(repository, storage, speech, transactions);
        ReflectionTestUtils.setField(worker, "maxAttempts", 3);

        worker.process(100L);

        verifyNoInteractions(storage, speech);
        verify(repository, never()).findById(anyLong());
    }
}

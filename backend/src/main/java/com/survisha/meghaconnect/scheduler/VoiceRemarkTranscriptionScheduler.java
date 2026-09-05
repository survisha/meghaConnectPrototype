package com.survisha.meghaconnect.scheduler;

import com.survisha.meghaconnect.entity.VoiceRemark;
import com.survisha.meghaconnect.repository.VoiceRemarkRepository;
import com.survisha.meghaconnect.service.VoiceTranscriptionQueue;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.List;

@Component @RequiredArgsConstructor
public class VoiceRemarkTranscriptionScheduler {
    private final VoiceRemarkRepository repository;
    private final VoiceTranscriptionQueue queue;
    private final PlatformTransactionManager transactionManager;
    @Value("${speech.retry.max-attempts:3}") private int maxAttempts;
    @Value("${speech.retry.delay-seconds:60}") private long retryDelaySeconds;
    @Value("${speech.queue.batch-size:100}") private int batchSize;
    @Value("${speech.processing-stale-seconds:300}") private long staleSeconds;

    @Scheduled(fixedDelayString = "${speech.worker-interval-ms:30000}", initialDelayString = "${speech.worker-initial-delay-ms:10000}")
    public void process() {
        LocalDateTime now = LocalDateTime.now();
        new TransactionTemplate(transactionManager).executeWithoutResult(status ->
                repository.recoverStaleProcessing(VoiceRemark.Status.PROCESSING, VoiceRemark.Status.PENDING,
                        now.minusSeconds(Math.max(30, staleSeconds)), now));
        LocalDateTime eligible = now.minusSeconds(Math.max(1, retryDelaySeconds));
        repository.findByTranscriptionStatusInAndTranscriptionAttemptsLessThanAndUpdatedAtBeforeOrderByCreatedAtAsc(
                List.of(VoiceRemark.Status.PENDING, VoiceRemark.Status.FAILED), maxAttempts, eligible,
                PageRequest.of(0, Math.max(1, batchSize))).forEach(item -> queue.submit(item.getId()));
    }
}

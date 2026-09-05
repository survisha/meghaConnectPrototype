package com.survisha.meghaconnect.service;

import com.survisha.meghaconnect.entity.VoiceRemark;
import com.survisha.meghaconnect.repository.VoiceRemarkRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class VoiceTranscriptionWorker {
    private final VoiceRemarkRepository repository;
    private final VoiceRemarkStorageService storage;
    private final SpeechTranscriptionClient speechClient;
    private final TransactionTemplate transactions;
    @Value("${speech.retry.max-attempts:3}") private int maxAttempts;

    public VoiceTranscriptionWorker(VoiceRemarkRepository repository, VoiceRemarkStorageService storage,
                                    SpeechTranscriptionClient speechClient, PlatformTransactionManager transactionManager) {
        this.repository = repository; this.storage = storage; this.speechClient = speechClient;
        this.transactions = new TransactionTemplate(transactionManager);
    }

    public void process(Long id) {
        Boolean claimed = transactions.execute(status -> repository.claimForProcessing(id, VoiceRemark.Status.PROCESSING,
                List.of(VoiceRemark.Status.PENDING, VoiceRemark.Status.FAILED), maxAttempts, LocalDateTime.now()) == 1);
        if (!Boolean.TRUE.equals(claimed)) return;

        VoiceRemark item = repository.findById(id).orElse(null);
        if (item == null) return;
        try {
            SpeechTranscriptionClient.Result result = speechClient.transcribe(storage.resolve(item.getAudioFilePath()));
            if (result.getText() == null || result.getText().isBlank()) throw new IllegalStateException("No speech was detected.");
            transactions.executeWithoutResult(status -> complete(id, result));
        } catch (Exception exception) {
            transactions.executeWithoutResult(status -> fail(id, exception));
        }
    }

    private void complete(Long id, SpeechTranscriptionClient.Result result) {
        VoiceRemark item = repository.findById(id).orElseThrow();
        item.setOriginalTranscript(result.getText());
        item.setDetectedLanguage(result.getLanguage()); item.setNeedsReview(result.isNeedsReview());
        item.setTranscriptionError(result.getWarning()); item.setTranscriptionStatus(VoiceRemark.Status.COMPLETED);
        item.setTranscribedAt(LocalDateTime.now()); item.setUpdatedAt(LocalDateTime.now()); repository.save(item);
    }

    private void fail(Long id, Exception exception) {
        VoiceRemark item = repository.findById(id).orElseThrow();
        item.setTranscriptionStatus(VoiceRemark.Status.FAILED); item.setNeedsReview(true);
        String message = exception.getMessage() == null ? exception.getClass().getSimpleName() : exception.getMessage();
        item.setTranscriptionError(message.substring(0, Math.min(1000, message.length())));
        item.setUpdatedAt(LocalDateTime.now()); repository.save(item);
    }
}

package com.survisha.meghaconnect.service;

import com.survisha.meghaconnect.dto.VoiceRemarkResponse;
import com.survisha.meghaconnect.entity.VoiceRemark;
import com.survisha.meghaconnect.repository.VoiceRemarkRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Service @RequiredArgsConstructor
public class VoiceRemarkService {
    private static final Set<String> REFERENCE_TYPES = Set.of("APPOINTMENT", "SCHEME", "HCM_ACTION", "APPROVAL_ACTION");
    private final VoiceRemarkRepository repository;
    private final VoiceRemarkStorageService storage;
    private final SpeechTranscriptionClient speechClient;
    @Value("${speech.retry.max-attempts:3}") private int maxAttempts;
    @Value("${speech.retry.delay-seconds:60}") private long retryDelaySeconds;
    @Value("${speech.queue.batch-size:4}") private int batchSize;

    @Transactional
    public VoiceRemarkResponse upload(MultipartFile audio, String referenceType, String referenceId, String requestId,
                                      Long durationMs, String actor, String role) {
        String type = normalizeReferenceType(referenceType);
        String ref = required(referenceId, "referenceId", 100);
        String key = required(requestId, "requestId", 64);
        return repository.findByRecordedByAndRequestId(actor, key).map(VoiceRemarkResponse::from).orElseGet(() -> {
            VoiceRemarkStorageService.StoredAudio stored = storage.store(audio, durationMs);
            LocalDateTime now = LocalDateTime.now();
            VoiceRemark item = new VoiceRemark();
            item.setRequestId(key); item.setReferenceType(type); item.setReferenceId(ref);
            item.setAudioFilePath(stored.getAbsolutePath()); item.setAudioFileName(stored.getFileName());
            item.setOriginalFileName(stored.getOriginalName()); item.setAudioFormat(stored.getFormat());
            item.setAudioDurationMs(durationMs); item.setAudioSizeBytes(stored.getSize());
            item.setRecordedBy(actor); item.setRecordedRole(role); item.setRecordedAt(now);
            item.setTranscriptionStatus(VoiceRemark.Status.PENDING); item.setCreatedAt(now); item.setUpdatedAt(now);
            return VoiceRemarkResponse.from(repository.save(item));
        });
    }

    @Transactional(readOnly = true)
    public VoiceRemarkResponse get(Long id) { return VoiceRemarkResponse.from(requireAccessible(id)); }

    @Transactional(readOnly = true)
    public List<VoiceRemarkResponse> list(String referenceType, String referenceId) {
        return repository.findByReferenceTypeAndReferenceIdOrderByRecordedAtDesc(normalizeReferenceType(referenceType), required(referenceId, "referenceId", 100))
                .stream().map(VoiceRemarkResponse::from).toList();
    }

    @Transactional
    public VoiceRemarkResponse retry(Long id) {
        VoiceRemark item = requireAccessible(id);
        if (item.getTranscriptionStatus() == VoiceRemark.Status.PROCESSING) throw new IllegalStateException("Transcription is already processing.");
        item.setTranscriptionStatus(VoiceRemark.Status.PENDING); item.setTranscriptionError(null); item.setTranscriptionAttempts(0);
        item.setUpdatedAt(LocalDateTime.now()); return VoiceRemarkResponse.from(repository.save(item));
    }

    @Transactional(readOnly = true)
    public Path audioPath(Long id) { return storage.resolve(requireAccessible(id).getAudioFilePath()); }

    @Transactional(readOnly = true)
    public VoiceRemark audioMetadata(Long id) { return requireAccessible(id); }

    public synchronized void processPending() {
        LocalDateTime eligible = LocalDateTime.now().minusSeconds(Math.max(1, retryDelaySeconds));
        List<VoiceRemark> jobs = repository.findByTranscriptionStatusInAndTranscriptionAttemptsLessThanAndUpdatedAtBeforeOrderByCreatedAtAsc(
                List.of(VoiceRemark.Status.PENDING, VoiceRemark.Status.FAILED), maxAttempts, eligible, PageRequest.of(0, Math.max(1, batchSize)));
        jobs.forEach(job -> process(job.getId()));
    }

    @Transactional
    public void process(Long id) {
        VoiceRemark item = repository.findById(id).orElse(null);
        if (item == null || item.getTranscriptionAttempts() >= maxAttempts) return;
        item.setTranscriptionStatus(VoiceRemark.Status.PROCESSING);
        item.setTranscriptionAttempts(item.getTranscriptionAttempts() + 1);
        item.setLastAttemptAt(LocalDateTime.now()); item.setUpdatedAt(LocalDateTime.now()); repository.saveAndFlush(item);
        try {
            SpeechTranscriptionClient.Result result = speechClient.transcribe(storage.resolve(item.getAudioFilePath()));
            if (result.getText() == null || result.getText().isBlank()) throw new IllegalStateException("No speech was detected.");
            item.setOriginalTranscript(result.getText());
            // Raw text remains authoritative. Optional cleanup can populate cleanedTranscript later.
            item.setDetectedLanguage(result.getLanguage()); item.setNeedsReview(result.isNeedsReview());
            item.setTranscriptionError(result.getWarning()); item.setTranscriptionStatus(VoiceRemark.Status.COMPLETED);
            item.setTranscribedAt(LocalDateTime.now());
        } catch (Exception e) {
            item.setTranscriptionStatus(VoiceRemark.Status.FAILED);
            item.setTranscriptionError(safeError(e)); item.setNeedsReview(true);
        }
        item.setUpdatedAt(LocalDateTime.now()); repository.save(item);
    }

    private VoiceRemark requireAccessible(Long id) { return repository.findById(id).orElseThrow(() -> new IllegalArgumentException("Voice remark not found.")); }
    private String normalizeReferenceType(String value) {
        String type = required(value, "referenceType", 40).toUpperCase();
        if (!REFERENCE_TYPES.contains(type)) throw new IllegalArgumentException("Unsupported voice remark reference type.");
        return type;
    }
    private String required(String value, String name, int max) {
        String v = value == null ? "" : value.trim();
        if (v.isEmpty() || v.length() > max || v.contains("..") || v.contains("/") || v.contains("\\"))
            throw new IllegalArgumentException(name + " is invalid.");
        return v;
    }
    private String safeError(Exception e) {
        String value = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
        return value.substring(0, Math.min(1000, value.length()));
    }
}

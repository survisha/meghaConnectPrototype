package com.survisha.meghaconnect.service;

import com.survisha.meghaconnect.dto.VoiceRemarkResponse;
import com.survisha.meghaconnect.entity.VoiceRemark;
import com.survisha.meghaconnect.repository.VoiceRemarkRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
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
    private final ApplicationEventPublisher events;

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
            VoiceRemark saved = repository.save(item);
            events.publishEvent(new VoiceRemarkStoredEvent(saved.getId()));
            return VoiceRemarkResponse.from(saved);
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
        item.setUpdatedAt(LocalDateTime.now()); VoiceRemark saved = repository.save(item);
        events.publishEvent(new VoiceRemarkStoredEvent(saved.getId()));
        return VoiceRemarkResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public Path audioPath(Long id) { return storage.resolve(requireAccessible(id).getAudioFilePath()); }

    @Transactional(readOnly = true)
    public VoiceRemark audioMetadata(Long id) { return requireAccessible(id); }

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
}

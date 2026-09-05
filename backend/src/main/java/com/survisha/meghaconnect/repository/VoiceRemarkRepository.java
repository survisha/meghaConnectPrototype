package com.survisha.meghaconnect.repository;

import com.survisha.meghaconnect.entity.VoiceRemark;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface VoiceRemarkRepository extends JpaRepository<VoiceRemark, Long> {
    Optional<VoiceRemark> findByRecordedByAndRequestId(String recordedBy, String requestId);
    List<VoiceRemark> findByReferenceTypeAndReferenceIdOrderByRecordedAtDesc(String referenceType, String referenceId);
    List<VoiceRemark> findByTranscriptionStatusInAndTranscriptionAttemptsLessThanAndUpdatedAtBeforeOrderByCreatedAtAsc(
            Collection<VoiceRemark.Status> statuses, int attempts, LocalDateTime before, Pageable pageable);
}

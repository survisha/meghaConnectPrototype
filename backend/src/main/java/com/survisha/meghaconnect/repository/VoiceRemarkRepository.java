package com.survisha.meghaconnect.repository;

import com.survisha.meghaconnect.entity.VoiceRemark;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface VoiceRemarkRepository extends JpaRepository<VoiceRemark, Long> {
    Optional<VoiceRemark> findByRecordedByAndRequestId(String recordedBy, String requestId);
    List<VoiceRemark> findByReferenceTypeAndReferenceIdOrderByRecordedAtDesc(String referenceType, String referenceId);
    List<VoiceRemark> findByTranscriptionStatusInAndTranscriptionAttemptsLessThanAndUpdatedAtBeforeOrderByCreatedAtAsc(
            Collection<VoiceRemark.Status> statuses, int attempts, LocalDateTime before, Pageable pageable);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update VoiceRemark v set v.transcriptionStatus = :processing, " +
            "v.transcriptionAttempts = v.transcriptionAttempts + 1, v.lastAttemptAt = :now, v.updatedAt = :now " +
            "where v.id = :id and v.transcriptionStatus in :claimable and v.transcriptionAttempts < :maxAttempts")
    int claimForProcessing(@Param("id") Long id, @Param("processing") VoiceRemark.Status processing,
                           @Param("claimable") Collection<VoiceRemark.Status> claimable,
                           @Param("maxAttempts") int maxAttempts, @Param("now") LocalDateTime now);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update VoiceRemark v set v.transcriptionStatus = :pending, " +
            "v.transcriptionError = 'Recovered after interrupted transcription', v.updatedAt = :now " +
            "where v.transcriptionStatus = :processing and v.lastAttemptAt < :staleBefore")
    int recoverStaleProcessing(@Param("processing") VoiceRemark.Status processing, @Param("pending") VoiceRemark.Status pending,
                               @Param("staleBefore") LocalDateTime staleBefore, @Param("now") LocalDateTime now);
}

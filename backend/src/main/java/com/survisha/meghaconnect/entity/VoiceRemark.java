package com.survisha.meghaconnect.entity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "voice_remarks", uniqueConstraints =
        @UniqueConstraint(name = "uq_voice_remark_actor_request", columnNames = {"recorded_by", "request_id"}))
@Getter @Setter @NoArgsConstructor
public class VoiceRemark {
    public enum Status { UPLOADED, PENDING, PROCESSING, COMPLETED, FAILED }

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "request_id", nullable = false, length = 64) private String requestId;
    @Column(name = "reference_type", nullable = false, length = 40) private String referenceType;
    @Column(name = "reference_id", nullable = false, length = 100) private String referenceId;
    @Column(name = "audio_file_path", nullable = false, length = 1000) private String audioFilePath;
    @Column(name = "audio_file_name", nullable = false) private String audioFileName;
    @Column(name = "original_file_name") private String originalFileName;
    @Column(name = "audio_format", nullable = false, length = 30) private String audioFormat;
    @Column(name = "audio_duration_ms") private Long audioDurationMs;
    @Column(name = "audio_size_bytes", nullable = false) private Long audioSizeBytes;
    @Column(name = "recorded_by", nullable = false) private String recordedBy;
    @Column(name = "recorded_role", nullable = false, length = 50) private String recordedRole;
    @Column(name = "recorded_at", nullable = false) private LocalDateTime recordedAt;
    @Enumerated(EnumType.STRING) @Column(name = "transcription_status", nullable = false, length = 20) private Status transcriptionStatus;
    @Column(name = "detected_language", length = 30) private String detectedLanguage;
    @Lob @Column(name = "original_transcript") private String originalTranscript;
    @Lob @Column(name = "cleaned_transcript") private String cleanedTranscript;
    @Column(name = "transcription_attempts", nullable = false) private int transcriptionAttempts;
    @Column(name = "transcription_error", length = 1000) private String transcriptionError;
    @Column(name = "last_attempt_at") private LocalDateTime lastAttemptAt;
    @Column(name = "transcribed_at") private LocalDateTime transcribedAt;
    @Lob @Column(name = "approved_text") private String approvedText;
    @Column(name = "needs_review", nullable = false) private boolean needsReview;
    @Column(name = "created_at", nullable = false) private LocalDateTime createdAt;
    @Column(name = "updated_at", nullable = false) private LocalDateTime updatedAt;
}

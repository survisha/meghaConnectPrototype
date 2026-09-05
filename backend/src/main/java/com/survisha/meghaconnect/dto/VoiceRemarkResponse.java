package com.survisha.meghaconnect.dto;

import com.survisha.meghaconnect.entity.VoiceRemark;
import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;

@Value @Builder
public class VoiceRemarkResponse {
    Long voiceRemarkId;
    boolean audioStored;
    String referenceType;
    String referenceId;
    String transcriptionStatus;
    String transcript;
    String originalTranscript;
    String detectedLanguage;
    boolean needsReview;
    int transcriptionAttempts;
    LocalDateTime recordedAt;
    String message;

    public static VoiceRemarkResponse from(VoiceRemark value) {
        boolean complete = value.getTranscriptionStatus() == VoiceRemark.Status.COMPLETED;
        return VoiceRemarkResponse.builder().voiceRemarkId(value.getId()).audioStored(true)
                .referenceType(value.getReferenceType()).referenceId(value.getReferenceId())
                .transcriptionStatus(value.getTranscriptionStatus().name())
                .transcript(complete ? (value.getCleanedTranscript() != null ? value.getCleanedTranscript() : value.getOriginalTranscript()) : null)
                .originalTranscript(complete ? value.getOriginalTranscript() : null)
                .detectedLanguage(value.getDetectedLanguage()).needsReview(value.isNeedsReview())
                .transcriptionAttempts(value.getTranscriptionAttempts()).recordedAt(value.getRecordedAt())
                .message(complete ? "Transcription is ready for review." : "Audio saved successfully. Transcription is being processed.")
                .build();
    }
}

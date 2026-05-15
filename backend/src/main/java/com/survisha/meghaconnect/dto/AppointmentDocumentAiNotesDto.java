package com.survisha.meghaconnect.dto;

import com.survisha.meghaconnect.entity.AppointmentDocumentAiNotes;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AppointmentDocumentAiNotesDto {

    private Long id;
    private Long appointmentId;
    private Long documentId;
    private String fileName;
    private String aiSummary;
    private String importantDetails;
    private String missingInfo;
    private String riskFlags;
    private AppointmentDocumentAiNotes.AiNoteStatus status;
    private String errorMessage;
    private String modelName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

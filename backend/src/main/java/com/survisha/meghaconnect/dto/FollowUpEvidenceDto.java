package com.survisha.meghaconnect.dto;

import lombok.*;
import java.time.LocalDateTime;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class FollowUpEvidenceDto {
    private Long id;
    private Long followUpId;
    private String filename;
    private String documentType;
    private String contentType;
    private Long fileSizeBytes;
    private String uploadedBy;
    private LocalDateTime uploadedDate;
}

package com.survisha.meghaconnect.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AppointmentDocumentDto {
    private Long id;
    private Long appointmentId;
    private String documentType;
    private String fileName;
    @JsonIgnore
    private String filePath;
    private Long fileSize;
    private String mimeType;
    private String uploadedBy;
    private LocalDateTime uploadedAt;
    private Boolean isRequired;
    private String status;
}

package com.survisha.meghaconnect.dto;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SchemeDocumentDto {
    private Long id;
    private String documentType;
    private String documentLabel;
    private Boolean isRequired;
    private String description;
    private String fileFormatAllowed;
    private Integer displayOrder;
    private String createdBy;
    private String updatedBy;
}

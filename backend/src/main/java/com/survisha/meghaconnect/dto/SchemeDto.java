package com.survisha.meghaconnect.dto;

import lombok.*;
import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SchemeDto {
    private Long id;
    private String schemeCode;
    private String schemeName;
    private String description;
    private Boolean isActive;
    private List<SchemeDocumentDto> requiredDocuments;
    private String createdBy;
    private String updatedBy;
}

package com.survisha.meghaconnect.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PilotImportResultDto {
    private Boolean success;
    private Integer totalRows;
    private Integer importedCount;
    private Integer failedCount;
    private String message;
    private List<PilotImportRowResultDto> rows;
}

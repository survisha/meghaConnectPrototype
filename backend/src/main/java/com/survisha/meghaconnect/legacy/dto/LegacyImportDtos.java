package com.survisha.meghaconnect.legacy.dto;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

public final class LegacyImportDtos {
    private LegacyImportDtos() {}

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class BatchSummary {
        private Long batchId; private String fileName; private String status; private String uploadedBy;
        private LocalDateTime uploadedAt; private int totalSheets; private int analyzedSheets;
        private int importedSheets; private int failedSheets; private int skippedSheets; private int mappingRequiredSheets;
        private long totalRows; private long validRows; private long importedRows; private long failedRows; private long duplicateRows;
        @Builder.Default private List<SheetSummary> sheets = new ArrayList<>();
    }
    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class SheetSummary {
        private Long id; private int sheetIndex; private String sheetName; private boolean hidden;
        private Integer detectedHeaderRow; private int columnCount; private long rowCount;
        private Long detectedDatasetId; private Long confirmedDatasetId; private String dataset;
        private String targetTable; private BigDecimal confidence; private long validRows;
        private long importedRows; private long failedRows; private long duplicateRows; private long skippedRows;
        private String status; private String statusReason;
        @Builder.Default private List<ColumnInfo> columns = new ArrayList<>();
    }
    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class ColumnInfo {
        private Long id; private int index; private String sourceHeader; private String normalizedHeader;
        private String detectedType; private String targetField; private String identifierType;
        private boolean mandatory; private boolean ignored; private String mappingStatus;
    }
    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class Preview {
        private Long sheetId; private String sheetName; private long totalRows;
        @Builder.Default private List<String> columns = new ArrayList<>();
        @Builder.Default private List<List<String>> rows = new ArrayList<>();
    }
    @Data @NoArgsConstructor @AllArgsConstructor
    public static class MappingRequest { private Long datasetId; private Integer headerRow; private List<ColumnMapping> columns; }
    @Data @NoArgsConstructor @AllArgsConstructor
    public static class ColumnMapping { private Integer sourceColumnIndex; private String targetField; private Boolean ignored; }
    @Data @NoArgsConstructor @AllArgsConstructor
    public static class DatasetRequest {
        private String datasetCode; private String datasetName; private String description; private String category;
        private String duplicateKeyFields; private Boolean approved; private List<DatasetColumnRequest> columns;
    }
    @Data @NoArgsConstructor @AllArgsConstructor
    public static class DatasetColumnRequest {
        private String targetFieldName; private String targetDataType; private Boolean mandatory;
        private String identifierType; private Integer displayOrder; private List<String> aliases;
    }
    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class DatasetInfo { private Long id; private String code; private String name; private String category; private boolean approved; private List<ColumnInfo> columns; }
    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class ErrorInfo {
        private Long id; private Long sheetId; private String sheetName; private long rowNumber;
        private String columnName; private String rawValue; private String errorCode; private String errorMessage;
    }
}

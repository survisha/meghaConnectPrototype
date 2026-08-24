package com.survisha.meghaconnect.legacy.entity;

import lombok.*;
import javax.persistence.*;
import java.time.LocalDateTime;

@Entity @Table(name="legacy_import_error")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class LegacyImportError {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(name="import_batch_id", nullable=false) private Long importBatchId;
    @Column(name="import_sheet_id", nullable=false) private Long importSheetId;
    @Column(name="sheet_name", nullable=false) private String sheetName;
    @Column(name="source_row_number", nullable=false) private long sourceRowNumber;
    @Column(name="column_name") private String columnName;
    @Column(name="raw_value", length=500) private String rawValue;
    @Column(name="error_code", nullable=false, length=60) private String errorCode;
    @Column(name="error_message", nullable=false, length=500) private String errorMessage;
    @Column(name="created_at", nullable=false) private LocalDateTime createdAt;
}

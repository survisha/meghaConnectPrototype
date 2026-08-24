package com.survisha.meghaconnect.legacy.entity;

import lombok.*;
import javax.persistence.*;
import java.time.LocalDateTime;

@Entity @Table(name="legacy_import_batch")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class LegacyImportBatch {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(name="original_file_name", nullable=false) private String originalFileName;
    @Column(name="stored_file_name", nullable=false) private String storedFileName;
    @Column(name="file_hash", nullable=false, length=64) private String fileHash;
    @Column(name="uploaded_by", nullable=false, length=100) private String uploadedBy;
    @Column(name="uploaded_at", nullable=false) private LocalDateTime uploadedAt;
    @Column(name="total_sheets", nullable=false) private int totalSheets;
    @Column(name="analyzed_sheets", nullable=false) private int analyzedSheets;
    @Column(name="imported_sheets", nullable=false) private int importedSheets;
    @Column(name="failed_sheets", nullable=false) private int failedSheets;
    @Column(name="skipped_sheets", nullable=false) private int skippedSheets;
    @Column(name="mapping_required_sheets", nullable=false) private int mappingRequiredSheets;
    @Column(name="total_rows", nullable=false) private long totalRows;
    @Column(name="valid_rows", nullable=false) private long validRows;
    @Column(name="imported_rows", nullable=false) private long importedRows;
    @Column(name="failed_rows", nullable=false) private long failedRows;
    @Column(name="duplicate_rows", nullable=false) private long duplicateRows;
    @Column(name="overall_status", nullable=false, length=30) private String overallStatus;
    @Column(name="started_at") private LocalDateTime startedAt;
    @Column(name="completed_at") private LocalDateTime completedAt;
}

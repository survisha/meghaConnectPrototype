package com.survisha.meghaconnect.legacy.entity;

import lombok.*;
import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity @Table(name="legacy_import_sheet")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class LegacyImportSheet {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch=FetchType.LAZY, optional=false) @JoinColumn(name="import_batch_id") private LegacyImportBatch batch;
    @Column(name="sheet_index", nullable=false) private int sheetIndex;
    @Column(name="sheet_name", nullable=false) private String sheetName;
    @Column(nullable=false) private boolean hidden;
    @Column(name="detected_header_row") private Integer detectedHeaderRow;
    @Column(name="total_columns", nullable=false) private int totalColumns;
    @Column(name="total_rows", nullable=false) private long totalRows;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="detected_dataset_id") private LegacyDatasetDefinition detectedDataset;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="confirmed_dataset_id") private LegacyDatasetDefinition confirmedDataset;
    @Column(name="target_table", length=80) private String targetTable;
    @Column(name="mapping_confidence", precision=5, scale=2) private BigDecimal mappingConfidence;
    @Column(name="valid_rows", nullable=false) private long validRows;
    @Column(name="imported_rows", nullable=false) private long importedRows;
    @Column(name="failed_rows", nullable=false) private long failedRows;
    @Column(name="duplicate_rows", nullable=false) private long duplicateRows;
    @Column(name="skipped_rows", nullable=false) private long skippedRows;
    @Column(nullable=false, length=30) private String status;
    @Column(name="status_reason") private String statusReason;
    @Column(name="started_at") private LocalDateTime startedAt;
    @Column(name="completed_at") private LocalDateTime completedAt;
}

package com.survisha.meghaconnect.legacy.entity;

import lombok.*;
import javax.persistence.*;
import java.time.LocalDateTime;

@Entity @Table(name="legacy_dataset_record")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class LegacyDatasetRecord {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch=FetchType.LAZY, optional=false) @JoinColumn(name="dataset_definition_id") private LegacyDatasetDefinition datasetDefinition;
    @Column(name="dataset_code", nullable=false, length=80) private String datasetCode;
    @Column(name="record_fingerprint", nullable=false, length=64) private String recordFingerprint;
    @Column(name="record_data", nullable=false, columnDefinition="json") private String recordData;
    @Column(name="source_file", nullable=false) private String sourceFile;
    @Column(name="source_sheet", nullable=false) private String sourceSheet;
    @Column(name="source_row_number", nullable=false) private long sourceRowNumber;
    @Column(name="import_batch_id", nullable=false) private Long importBatchId;
    @Column(name="imported_by", nullable=false, length=100) private String importedBy;
    @Column(name="imported_at", nullable=false) private LocalDateTime importedAt;
}

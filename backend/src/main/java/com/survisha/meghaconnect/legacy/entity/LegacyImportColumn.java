package com.survisha.meghaconnect.legacy.entity;

import lombok.*;
import javax.persistence.*;

@Entity @Table(name="legacy_import_column")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class LegacyImportColumn {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch=FetchType.LAZY, optional=false) @JoinColumn(name="import_sheet_id") private LegacyImportSheet sheet;
    @Column(name="source_column_index", nullable=false) private int sourceColumnIndex;
    @Column(name="source_column_name", nullable=false) private String sourceColumnName;
    @Column(name="normalized_column_name", nullable=false, length=160) private String normalizedColumnName;
    @Column(name="detected_data_type", nullable=false, length=20) private String detectedDataType;
    @Column(name="mapped_target_field", length=80) private String mappedTargetField;
    @Column(name="mapped_identifier_type", length=30) private String mappedIdentifierType;
    @Column(nullable=false) private boolean mandatory;
    @Column(nullable=false) private boolean ignored;
    @Column(name="mapping_status", nullable=false, length=30) private String mappingStatus;
}

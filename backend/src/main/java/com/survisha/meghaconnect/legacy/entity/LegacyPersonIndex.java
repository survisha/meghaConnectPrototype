package com.survisha.meghaconnect.legacy.entity;

import lombok.*;
import javax.persistence.*;

@Entity @Table(name="legacy_person_index")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class LegacyPersonIndex {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(name="source_dataset_code", nullable=false, length=80) private String sourceDatasetCode;
    @Column(name="source_table", nullable=false, length=80) private String sourceTable;
    @Column(name="source_record_id", nullable=false) private Long sourceRecordId;
    @Column private String name;
    @Column(name="normalized_name") private String normalizedName;
    @Column(length=50) private String epic;
    @Column(name="normalized_epic", length=50) private String normalizedEpic;
    @Column(length=20) private String mobile;
    @Column(name="normalized_mobile", length=20) private String normalizedMobile;
    @Column(length=180) private String village;
    @Column(name="normalized_village", length=180) private String normalizedVillage;
    @Column(length=500) private String address;
    @Column(name="normalized_address", length=500) private String normalizedAddress;
    @Column(length=120) private String district;
    @Column(length=120) private String constituency;
    @Column(name="scheme_code", length=80) private String schemeCode;
    @Column(name="identity_basis", nullable=false, length=20) private String identityBasis;
    @Column(name="source_file", nullable=false) private String sourceFile;
    @Column(name="source_sheet", nullable=false) private String sourceSheet;
    @Column(name="source_row_number", nullable=false) private long sourceRowNumber;
    @Column(name="import_batch_id", nullable=false) private Long importBatchId;
}

package com.survisha.meghaconnect.legacy.entity;

import lombok.*;
import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity @Table(name = "legacy_dataset_definition")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class LegacyDatasetDefinition {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name="dataset_code", nullable=false, unique=true, length=80) private String datasetCode;
    @Column(name="dataset_name", nullable=false, length=160) private String datasetName;
    @Column(length=500) private String description;
    @Column(name="target_table", nullable=false, length=80) private String targetTable;
    @Column(length=80) private String category;
    @Column(name="duplicate_key_fields", length=500) private String duplicateKeyFields;
    @Column(nullable=false) private boolean active;
    @Column(nullable=false) private boolean approved;
    @Column(name="created_by", nullable=false, length=100) private String createdBy;
    @Column(name="created_at", nullable=false) private LocalDateTime createdAt;
    @Column(name="updated_by", length=100) private String updatedBy;
    @Column(name="updated_at") private LocalDateTime updatedAt;
    @OneToMany(mappedBy="datasetDefinition", cascade=CascadeType.ALL, orphanRemoval=true)
    @OrderBy("displayOrder ASC") @Builder.Default
    private List<LegacyDatasetColumn> columns = new ArrayList<>();
}

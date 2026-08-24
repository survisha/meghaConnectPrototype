package com.survisha.meghaconnect.legacy.entity;

import lombok.*;
import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity @Table(name="legacy_dataset_column")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class LegacyDatasetColumn {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch=FetchType.LAZY, optional=false) @JoinColumn(name="dataset_definition_id") private LegacyDatasetDefinition datasetDefinition;
    @Column(name="target_field_name", nullable=false, length=80) private String targetFieldName;
    @Column(name="target_data_type", nullable=false, length=20) private String targetDataType;
    @Column(nullable=false) private boolean mandatory;
    @Column(name="identifier_type", nullable=false, length=30) private String identifierType;
    @Column(name="display_order", nullable=false) private int displayOrder;
    @Column(nullable=false) private boolean active;
    @OneToMany(mappedBy="datasetColumn", cascade=CascadeType.ALL, orphanRemoval=true) @Builder.Default
    private List<LegacyDatasetColumnAlias> aliases = new ArrayList<>();
}

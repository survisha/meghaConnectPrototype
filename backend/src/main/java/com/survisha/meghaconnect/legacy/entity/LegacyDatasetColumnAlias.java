package com.survisha.meghaconnect.legacy.entity;

import lombok.*;
import javax.persistence.*;

@Entity @Table(name="legacy_dataset_column_alias")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class LegacyDatasetColumnAlias {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch=FetchType.LAZY, optional=false) @JoinColumn(name="dataset_column_id") private LegacyDatasetColumn datasetColumn;
    @Column(name="source_column_alias", nullable=false, length=160) private String sourceColumnAlias;
    @Column(name="normalized_alias", nullable=false, length=160) private String normalizedAlias;
}

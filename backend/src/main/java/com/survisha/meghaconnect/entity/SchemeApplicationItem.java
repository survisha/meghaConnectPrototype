package com.survisha.meghaconnect.entity;

import javax.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "scheme_application_items")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SchemeApplicationItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "scheme_application_id", nullable = false)
    private SchemeApplication schemeApplication;

    @Column(nullable = false, length = 300)
    private String description;

    @Column(nullable = false)
    private Integer quantity;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal unitCost;

    @Column(precision = 14, scale = 2)
    private BigDecimal cmoModeratedUnitCost;

    @Column(precision = 14, scale = 2)
    private BigDecimal hcmApprovedUnitCost;
}

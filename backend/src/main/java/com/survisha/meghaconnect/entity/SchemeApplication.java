package com.survisha.meghaconnect.entity;

import javax.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.util.List;

import com.survisha.meghaconnect.entity.Visitor;

@Entity
@Table(name = "scheme_applications",
    indexes = { @Index(name = "idx_scheme_applicant", columnList = "applicant_id") })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SchemeApplication extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "applicant_id", nullable = false)
    private Visitor applicant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "appointment_id")
    private Appointment appointment;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SchemeType schemeType;

    @Column(nullable = false, length = 300)
    private String projectName;

    @Column(length = 100)
    private String projectCategory;

    @Column(length = 100)
    private String beneficiaryType;

    @Column(length = 50)
    private String beneficiaryCount;

    @Column(precision = 14, scale = 2)
    private BigDecimal estimatedCost;

    @Column(precision = 14, scale = 2)
    private BigDecimal communityContribution;

    @Column(columnDefinition = "TEXT")
    private String justification;

    // CMO moderation
    @Column(precision = 14, scale = 2)
    private BigDecimal cmoModeratedCost;

    // HCM decision
    @Enumerated(EnumType.STRING)
    private HcmDecision hcmDecision;

    @Column(precision = 14, scale = 2)
    private BigDecimal hcmApprovedCost;

    @Column(columnDefinition = "TEXT")
    private String hcmRemarks;

    @Column(length = 50)
    private String status;

    @OneToMany(mappedBy = "schemeApplication", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<SchemeApplicationItem> items;

    public enum SchemeType {
        CMSDF, CMSG, CM_CARE, CM_CONNECT, CM_ELEVATE, FOCUS_PLUS, OTHERS
    }

    public enum HcmDecision {
        APPROVED, REJECTED, APPROVED_WITH_CHANGES
    }
}

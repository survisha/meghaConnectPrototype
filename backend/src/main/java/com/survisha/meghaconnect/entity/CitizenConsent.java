package com.survisha.meghaconnect.entity;

import javax.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "citizen_consents")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class CitizenConsent {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "visitor_id", nullable = false)
    private Visitor visitor;
    @Column(name = "consent_purposes", nullable = false, length = 100)
    private String consentPurposes;
    @Column(name = "consent_version", nullable = false, length = 50)
    private String consentVersion;
    @Column(name = "consent_text", nullable = false, length = 1000)
    private String consentText;
    @Column(name = "consent_granted", nullable = false)
    private Boolean consentGranted;
    @Column(name = "consented_at", nullable = false)
    private LocalDateTime consentedAt;
    @Column(nullable = false, length = 20)
    private String channel;
    @Column(name = "recorded_by", length = 100)
    private String recordedBy;
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}

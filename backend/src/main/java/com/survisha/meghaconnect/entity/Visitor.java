package com.survisha.meghaconnect.entity;

import javax.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.util.List;

@Entity
@Table(name = "visitors",
    indexes = {
        @Index(name = "idx_visitor_phone", columnList = "phoneNumber"),
        @Index(name = "idx_visitor_epic",  columnList = "epicNumber"),
    })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Visitor extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String fullName;

    @Column(length = 20)
    private String phoneNumber;

    @Column(length = 150)
    private String email;

    @Column(length = 50)
    private String epicNumber;

    /** Aadhaar number – used as KYC fallback when EPIC is not available. */
    @Column(length = 20)
    private String aadhaarNumber;

    /** Which KYC document was verified: EPIC, AADHAR, or NONE. */
    @Column(length = 10)
    private String kycType;

    private Boolean kycVerified;

    private java.time.LocalDateTime kycVerifiedAt;

    /**
     * Path / object-key in the configured file store.
     * The base URL is resolved at runtime from application.yml
     * (meghaconnect.storage.base-url).  Example:
     *   persons/42/photo.jpg
     */
    @Column(length = 500)
    private String photoStoragePath;

    /** Legacy short path – kept for backward-compat; prefer photoStoragePath. */
    @Column(length = 200)
    private String photoPath;

    @Column(length = 100)
    private String designation;

    @Column(length = 100)
    private String district;

    @Column(length = 100)
    private String constituency;

    @Column(length = 100)
    private String booth;

    @Column(length = 100)
    private String village;

    @Column(columnDefinition = "TEXT")
    private String briefProfile;

    private LocalDate dateOfBirth;

    @Column(length = 500)
    private String address;

    // Facial recognition embedding reference
    @Column(length = 500)
    private String faceEmbeddingRef;

    /**
     * Granular KYC status:
     * PENDING | PHOTO_MATCHED | DEMOGRAPHIC_MATCHED | FAILED | NOT_VERIFIED | MANUAL_VERIFICATION_REQUIRED
     */
    @Column(length = 50)
    private String kycStatus;

    @OneToMany(mappedBy = "applicant", fetch = FetchType.LAZY)
    private List<Appointment> appointments;
}

package com.survisha.meghaconnect.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.survisha.meghaconnect.util.DateTimeUtil;
import javax.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "document_uploads",
    indexes = {
        @Index(name = "idx_appointment_id_doc", columnList = "appointment_id"),
        @Index(name = "idx_visitor_id_doc", columnList = "visitor_id"),
    })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class DocumentUpload extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "visitor_id")
    private Visitor visitor;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "appointment_id")
    private Appointment appointment;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "scheme_app_id")
    private SchemeApplication schemeApplication;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "follow_up_id")
    private DirectionFollowUp followUp;

    @Column(nullable = false, length = 100)
    private String documentType;  // e.g., EPIC_SCAN, APPLICATION_LETTER, etc.

    @Column(length = 300)
    private String originalFilename;

    @Column(length = 300)
    private String storedFileName;

    @Column(nullable = false, length = 1000)
    @JsonIgnore
    private String filePath;  // Legacy column; new rows store an encrypted path value.

    @Column(length = 1000)
    @JsonIgnore
    private String encryptedFilePath;

    @Column(length = 128)
    @JsonIgnore
    private String secureHash;

    private Long fileSizeBytes;

    @Column(length = 100)
    private String mimeType;

    @Column(length = 100)
    private String contentType;

    @Column(length = 100)
    private String uploadedBy;

    private LocalDateTime uploadedDate;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = DateTimeUtil.nowIST();
        }
        if (uploadedDate == null) {
            uploadedDate = createdAt;
        }
        if (updatedAt == null) {
            updatedAt = DateTimeUtil.nowIST();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = DateTimeUtil.nowIST();
    }
}

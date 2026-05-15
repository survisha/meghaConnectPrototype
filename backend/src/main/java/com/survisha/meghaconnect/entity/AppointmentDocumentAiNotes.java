package com.survisha.meghaconnect.entity;

import javax.persistence.*;
import lombok.*;

@Entity
@Table(name = "appointment_document_ai_notes",
        indexes = {
                @Index(name = "idx_ai_notes_appointment", columnList = "appointment_id"),
                @Index(name = "idx_ai_notes_status", columnList = "status")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_ai_notes_document", columnNames = "document_id")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AppointmentDocumentAiNotes extends BaseEntity {

    public enum AiNoteStatus {
        PENDING,
        PROCESSING,
        COMPLETED,
        FAILED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "appointment_id", nullable = false)
    private Appointment appointment;

    @Column(name = "appointment_id", insertable = false, updatable = false)
    private Long appointmentId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "document_id", nullable = false)
    private DocumentUpload document;

    @Column(name = "document_id", insertable = false, updatable = false)
    private Long documentId;

    @Column(length = 300)
    private String fileName;

    @Lob
    private String aiSummary;

    @Lob
    private String importantDetails;

    @Lob
    private String missingInfo;

    @Lob
    private String riskFlags;

    @Lob
    private String rawAiResponse;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private AiNoteStatus status;

    @Lob
    private String errorMessage;

    @Column(length = 100)
    private String modelName;
}

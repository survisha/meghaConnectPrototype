package com.survisha.meghaconnect.entity;

import lombok.*;

import javax.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "direction_follow_ups", indexes = {
        @Index(name = "idx_followup_department_status_due", columnList = "department_id,status,due_date"),
        @Index(name = "idx_followup_appointment", columnList = "appointment_id"),
        @Index(name = "idx_followup_visitor", columnList = "visitor_id")
})
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class DirectionFollowUp extends BaseEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "direction_id", unique = true, length = 30)
    private String directionId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "appointment_id", nullable = false)
    private Appointment appointment;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "visitor_id", nullable = false)
    private Visitor visitor;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "department_id", nullable = false)
    private Department department;

    @Column(name = "responsible_officer_name", length = 200)
    private String responsibleOfficerName;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String instruction;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private FollowUpStatus status = FollowUpStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private Priority priority = Priority.NORMAL;

    @Column(name = "evidence_required", nullable = false)
    @Builder.Default
    private Boolean evidenceRequired = false;

    @Column(name = "last_escalated_at")
    private LocalDateTime lastEscalatedAt;

    @Column(name = "completed_date")
    private LocalDateTime completedDate;

    @Column(name = "completion_remarks", columnDefinition = "TEXT")
    private String completionRemarks;

    public enum FollowUpStatus { PENDING, IN_PROGRESS, COMPLETED }
    public enum Priority { LOW, NORMAL, HIGH, URGENT }

    @Transient
    public boolean isOverdue(LocalDate today) {
        return status != FollowUpStatus.COMPLETED && dueDate != null && dueDate.isBefore(today);
    }
}

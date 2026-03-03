package com.survisha.meghaconnect.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "grievances",
    indexes = {
        @Index(name = "idx_grievance_ticket",   columnList = "ticketId"),
        @Index(name = "idx_grievance_status",   columnList = "status"),
        @Index(name = "idx_grievance_phone",    columnList = "phoneNumber"),
    })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Grievance extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 30)
    private String ticketId;

    @Column(nullable = false, length = 200)
    private String applicantName;

    @Column(length = 20)
    private String phoneNumber;

    @Column(length = 100)
    private String district;

    @Column(length = 100)
    private String constituency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private GrievanceCategory category;

    @Column(nullable = false, length = 300)
    private String subject;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private GrievanceStatus status;

    @Column(nullable = false)
    private LocalDateTime submittedAt;

    private LocalDateTime resolvedAt;

    @Column(length = 200)
    private String assignedDepartment;

    @Column(columnDefinition = "TEXT")
    private String remarks;

    public enum GrievanceCategory {
        PUBLIC_SERVICES, INFRASTRUCTURE, HEALTH, EDUCATION,
        EMPLOYMENT, WELFARE_SCHEME, LAW_ORDER, OTHERS
    }

    public enum GrievanceStatus {
        SUBMITTED, ACKNOWLEDGED, UNDER_REVIEW, FORWARDED, RESOLVED, CLOSED
    }
}

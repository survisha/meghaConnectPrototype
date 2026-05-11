package com.survisha.meghaconnect.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import javax.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "grievances",
    indexes = {
        @Index(name = "idx_grievance_ticket",   columnList = "ticketId"),
        @Index(name = "idx_grievance_status",   columnList = "status"),
        @Index(name = "idx_grievance_visitor",  columnList = "visitor_id"),
    })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Grievance extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "visitor_id")
    private Visitor visitor;

    @Column(unique = true, nullable = false, length = 30)
    private String ticketId;

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

    @JsonIgnore
    public Visitor getVisitor() {
        return visitor;
    }

    public Long getVisitorId() {
        return visitor != null ? visitor.getId() : null;
    }

    public String getApplicantName() {
        return visitor != null ? visitor.getFullName() : null;
    }

    public String getPhoneNumber() {
        return visitor != null ? visitor.getPhoneNumber() : null;
    }

    public String getDistrict() {
        return visitor != null ? visitor.getDistrict() : null;
    }

    public String getConstituency() {
        return visitor != null ? visitor.getConstituency() : null;
    }

    public String getVisitorDesignation() {
        return visitor != null ? visitor.getDesignation() : null;
    }

    public GrievanceCategory getCategory() {
        return inferCategoryFromDesignation(getVisitorDesignation());
    }

    public enum GrievanceCategory {
        PUBLIC_SERVICES, INFRASTRUCTURE, HEALTH, EDUCATION,
        EMPLOYMENT, WELFARE_SCHEME, LAW_ORDER, OTHERS
    }

    public enum GrievanceStatus {
        SUBMITTED, ACKNOWLEDGED, UNDER_REVIEW, FORWARDED, RESOLVED, CLOSED
    }

    private GrievanceCategory inferCategoryFromDesignation(String designation) {
        String normalized = designation != null ? designation.trim().toLowerCase() : "";
        if (normalized.contains("teacher") || normalized.contains("student")) {
            return GrievanceCategory.EDUCATION;
        }
        if (normalized.contains("business")) {
            return GrievanceCategory.EMPLOYMENT;
        }
        if (normalized.contains("govt") || normalized.contains("government")) {
            return GrievanceCategory.PUBLIC_SERVICES;
        }
        return GrievanceCategory.OTHERS;
    }

}

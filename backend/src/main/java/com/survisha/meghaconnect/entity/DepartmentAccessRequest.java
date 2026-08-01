package com.survisha.meghaconnect.entity;

import lombok.*;
import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "department_access_requests")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class DepartmentAccessRequest extends BaseEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id")
    private Department department;
    @Column(nullable = false, length = 200) private String departmentName;
    @Column(nullable = false, length = 50) private String departmentCode;
    @Column(nullable = false, length = 150) private String nodalOfficerName;
    @Column(nullable = false, length = 150) private String officialEmail;
    @Column(nullable = false, length = 20) private String officialMobile;
    @Column(nullable = false, length = 500) private String requestPurpose;
    @Column(nullable = false) private int expectedUserCount;
    @Column(length = 1000) private String remarks;
    @Column(length = 500) private String supportingDocumentPath;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20)
    private Status requestStatus;
    @Column(nullable = false) private LocalDateTime submittedAt;
    private LocalDateTime reviewedAt;
    @Column(length = 100) private String reviewedBy;
    @Column(length = 500) private String rejectionReason;

    public enum Status { PENDING, APPROVED, REJECTED, CANCELLED }
}

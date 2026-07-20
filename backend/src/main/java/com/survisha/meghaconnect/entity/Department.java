package com.survisha.meghaconnect.entity;

import lombok.*;

import javax.persistence.*;

@Entity
@Table(name = "departments",
        indexes = {
                @Index(name = "idx_departments_code", columnList = "department_code"),
                @Index(name = "idx_departments_status", columnList = "status")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Department extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "department_code", nullable = false, unique = true, length = 50)
    private String departmentCode;

    @Column(name = "department_name", nullable = false, length = 200)
    private String departmentName;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "contact_email", length = 150)
    private String contactEmail;

    @Column(name = "contact_mobile", length = 20)
    private String contactMobile;

    @Column(length = 500)
    private String address;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DepartmentStatus status;

    public enum DepartmentStatus {
        ACTIVE,
        INACTIVE
    }
}

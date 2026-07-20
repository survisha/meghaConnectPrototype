package com.survisha.meghaconnect.dto;

import com.survisha.meghaconnect.entity.Department;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DepartmentDto {
    private Long id;
    private String departmentCode;
    private String departmentName;
    private String description;
    private String contactEmail;
    private String contactMobile;
    private String address;
    private Department.DepartmentStatus status;
    private String createdBy;
    private LocalDateTime createdAt;
    private String updatedBy;
    private LocalDateTime updatedAt;
}

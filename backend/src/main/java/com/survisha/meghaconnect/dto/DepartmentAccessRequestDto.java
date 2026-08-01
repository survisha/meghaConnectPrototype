package com.survisha.meghaconnect.dto;

import com.survisha.meghaconnect.entity.DepartmentAccessRequest;
import lombok.*;
import javax.validation.constraints.*;
import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class DepartmentAccessRequestDto {
    private Long id;
    private Long departmentId;
    @NotBlank @Size(max=200) private String departmentName;
    @NotBlank @Pattern(regexp="^[A-Za-z0-9_-]{2,50}$") private String departmentCode;
    @NotBlank @Size(max=150) private String nodalOfficerName;
    @NotBlank @Email @Size(max=150) private String officialEmail;
    @NotBlank @Pattern(regexp="^[0-9]{10}$") private String officialMobile;
    @NotBlank @Size(max=500) private String requestPurpose;
    @Min(1) @Max(10000) private int expectedUserCount;
    @Size(max=1000) private String remarks;
    private DepartmentAccessRequest.Status requestStatus;
    private LocalDateTime submittedAt;
    private LocalDateTime reviewedAt;
    private String reviewedBy;
    private String rejectionReason;
}

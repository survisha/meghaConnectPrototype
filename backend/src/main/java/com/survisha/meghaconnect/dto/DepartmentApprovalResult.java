package com.survisha.meghaconnect.dto;
import lombok.*;
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class DepartmentApprovalResult {
    private DepartmentAccessRequestDto request;
    private UserResponse departmentAdmin;
    /** Returned only in the approval response; it is never persisted or logged in plain text. */
    private String oneTimeTemporaryPassword;
}

package com.survisha.meghaconnect.dto;

import com.survisha.meghaconnect.entity.Department;
import lombok.*;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DepartmentRequest {

    @NotBlank
    @Size(max = 50)
    @Pattern(regexp = "^[A-Z0-9_]+$", message = "Department code must use uppercase letters, numbers, and underscores only")
    private String departmentCode;

    @NotBlank
    @Size(max = 200)
    private String departmentName;

    private String description;

    @Email
    @Size(max = 150)
    private String contactEmail;

    @Size(max = 20)
    @Pattern(regexp = "^$|^[0-9]{10}$", message = "Contact mobile must be exactly 10 digits")
    private String contactMobile;

    @Size(max = 500)
    private String address;

    private Department.DepartmentStatus status;
}

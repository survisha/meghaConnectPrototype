package com.survisha.meghaconnect.dto;

import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.Email;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

/** Public input contract. Workflow and review fields are intentionally excluded. */
@Getter
@Setter
public class CreateDepartmentAccessRequest {
    @NotBlank @Pattern(regexp = "^[A-Za-z0-9_-]{2,50}$")
    private String departmentCode;
    @NotBlank @Size(min = 2, max = 150)
    private String nodalOfficerName;
    @NotBlank @Email @Size(max = 150)
    private String officialEmail;
    @NotBlank @Pattern(regexp = "^[6-9][0-9]{9}$")
    private String officialMobile;
    @NotBlank @Size(min = 10, max = 500)
    private String requestPurpose;
    @Min(1) @Max(10000)
    private int expectedUserCount;
    @Size(max = 1000)
    private String remarks;
}

package com.survisha.meghaconnect.dto;
import lombok.Getter;
import lombok.Setter;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
@Getter @Setter
public class RejectDepartmentAccessRequest {
    @NotBlank @Size(max=500) private String rejectionReason;
}

package com.survisha.meghaconnect.dto;

import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;

@Getter
@Setter
public class ChangeTemporaryPasswordRequest {
    @NotBlank
    private String currentPassword;

    @NotBlank
    @Pattern(
            regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9]).{10,72}$",
            message = "Password must be 10-72 characters and include upper, lower, number, and special characters"
    )
    private String newPassword;
}

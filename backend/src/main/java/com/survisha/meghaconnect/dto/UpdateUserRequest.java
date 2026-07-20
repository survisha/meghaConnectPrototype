package com.survisha.meghaconnect.dto;

import com.survisha.meghaconnect.entity.User;
import lombok.*;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateUserRequest {

    @NotBlank
    @Size(max = 100)
    private String fullName;

    @Size(max = 150)
    @javax.validation.constraints.Email
    private String email;

    @NotNull
    private User.UserRole role;

    private Long departmentId;

    @Size(max = 20)
    @Pattern(regexp = "^$|^[0-9]{10}$", message = "Phone number must be exactly 10 digits")
    private String phoneNumber;

    private Boolean active;
    private Boolean locked;
    private Boolean offlineAccess;
}

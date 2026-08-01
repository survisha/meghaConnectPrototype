package com.survisha.meghaconnect.dto;

import javax.validation.constraints.NotBlank;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class AuthRequest {
    @NotBlank private String username;
    @NotBlank private String password;
    private String captchaId;
    private String captchaValue;

    public AuthRequest(String username, String password) {
        this.username = username;
        this.password = password;
    }
}

package com.survisha.meghaconnect.dto;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AuthResponse {
    private String token;
    private String refreshToken;
    private String username;
    private String fullName;
    private String role;
    private Long expiresIn;
}

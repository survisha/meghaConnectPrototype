package com.survisha.meghaconnect.dto;

import com.survisha.meghaconnect.util.RequestContextUtil;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AuthResponse {
    private String token;
    private String refreshToken;
    private String username;
    private String fullName;
    private String role;
    private Long userId;
    private Long departmentId;
    private String departmentCode;
    private String departmentName;
    private boolean passwordChangeRequired;
    private Long expiresIn;
    @Builder.Default
    private String requestId = RequestContextUtil.getRequestId();
}

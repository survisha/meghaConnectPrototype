package com.survisha.meghaconnect.controller;

import com.survisha.meghaconnect.dto.QrActionResponse;
import com.survisha.meghaconnect.dto.QrScanRequest;
import com.survisha.meghaconnect.dto.QrValidationResponse;
import com.survisha.meghaconnect.response.ApiResponse;
import com.survisha.meghaconnect.service.QrScannerService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/v1/visitor-pass")
@RequiredArgsConstructor
public class VisitorPassController {

    private final QrScannerService qrScannerService;

    @PostMapping("/validate")
    @PreAuthorize("hasAnyRole('SECURITY','ADMIN','SUPER_ADMIN')")
    public ApiResponse<QrValidationResponse> validate(@RequestBody QrScanRequest request,
                                                       Authentication authentication,
                                                       HttpServletRequest httpRequest) {
        normalizeQrData(request);
        return ApiResponse.success(
                "Visitor pass verified successfully",
                qrScannerService.validate(request, actor(authentication), role(authentication), clientIp(httpRequest), userAgent(httpRequest))
        );
    }

    @PostMapping("/check-in")
    @PreAuthorize("hasAnyRole('SECURITY','ADMIN','SUPER_ADMIN')")
    public ApiResponse<QrActionResponse> checkIn(@RequestBody QrScanRequest request,
                                                 Authentication authentication,
                                                 HttpServletRequest httpRequest) {
        normalizeQrData(request);
        return ApiResponse.success(
                "Check-in completed",
                qrScannerService.checkIn(request, actor(authentication), role(authentication), clientIp(httpRequest), userAgent(httpRequest))
        );
    }

    private void normalizeQrData(QrScanRequest request) {
        if (request != null && (request.getQrToken() == null || request.getQrToken().trim().isEmpty())) {
            request.setQrToken(request.getQrData());
        }
    }

    private String actor(Authentication authentication) {
        return authentication != null ? authentication.getName() : "anonymous";
    }

    private String role(Authentication authentication) {
        if (authentication == null || authentication.getAuthorities() == null) {
            return "ANONYMOUS";
        }
        return authentication.getAuthorities()
                .stream()
                .map(GrantedAuthority::getAuthority)
                .findFirst()
                .map(authority -> authority.replace("ROLE_", ""))
                .orElse("ANONYMOUS");
    }

    private String clientIp(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.trim().isEmpty()) {
            return forwardedFor.split(",")[0].trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        return realIp != null && !realIp.trim().isEmpty() ? realIp.trim() : request.getRemoteAddr();
    }

    private String userAgent(HttpServletRequest request) {
        return request != null ? request.getHeader("User-Agent") : null;
    }
}

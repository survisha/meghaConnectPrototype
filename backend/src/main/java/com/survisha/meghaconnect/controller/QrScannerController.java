package com.survisha.meghaconnect.controller;

import com.survisha.meghaconnect.dto.*;
import com.survisha.meghaconnect.entity.QrScanAuditLog;
import com.survisha.meghaconnect.response.ApiResponse;
import com.survisha.meghaconnect.service.QrReportService;
import com.survisha.meghaconnect.service.QrScannerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/qr")
@RequiredArgsConstructor
@Slf4j
public class QrScannerController {

    private final QrScannerService qrScannerService;
    private final QrReportService qrReportService;

    @PostMapping("/validate")
    @PreAuthorize("hasAnyRole('SECURITY','ADMIN','SUPER_ADMIN')")
    public ApiResponse<QrValidationResponse> validate(@RequestBody QrScanRequest request,
                                                       Authentication authentication,
                                                       HttpServletRequest httpRequest) {
        log.info("QR validate request received requestId={}", com.survisha.meghaconnect.util.RequestContextUtil.getRequestId());
        return ApiResponse.success(
                "QR validated",
                qrScannerService.validate(
                        request,
                        actor(authentication),
                        role(authentication),
                        clientIp(httpRequest),
                        userAgent(httpRequest)
                )
        );
    }

    @PostMapping("/check-in")
    @PreAuthorize("hasAnyRole('SECURITY','ADMIN','SUPER_ADMIN')")
    public ApiResponse<QrActionResponse> checkIn(@RequestBody QrScanRequest request,
                                                 Authentication authentication,
                                                 HttpServletRequest httpRequest) {
        log.info("QR check-in request received requestId={}", com.survisha.meghaconnect.util.RequestContextUtil.getRequestId());
        return ApiResponse.success(
                "Check-in completed",
                qrScannerService.checkIn(
                        request,
                        actor(authentication),
                        role(authentication),
                        clientIp(httpRequest),
                        userAgent(httpRequest)
                )
        );
    }

    @PostMapping("/check-out")
    @PreAuthorize("hasAnyRole('SECURITY','ADMIN','SUPER_ADMIN')")
    public ApiResponse<QrActionResponse> checkOut(@RequestBody QrScanRequest request,
                                                  Authentication authentication,
                                                  HttpServletRequest httpRequest) {
        log.info("QR check-out request received requestId={}", com.survisha.meghaconnect.util.RequestContextUtil.getRequestId());
        return ApiResponse.success(
                "Check-out completed",
                qrScannerService.checkOut(
                        request,
                        actor(authentication),
                        role(authentication),
                        clientIp(httpRequest),
                        userAgent(httpRequest)
                )
        );
    }

    @GetMapping("/recent-scans")
    @PreAuthorize("hasAnyRole('SECURITY','ADMIN','SUPER_ADMIN')")
    public ApiResponse<List<RecentQrScanDto>> recentScans(Authentication authentication) {
        return ApiResponse.success("Recent scans fetched", qrReportService.getRecentScans(actor(authentication)));
    }

    @GetMapping("/audit-logs")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ApiResponse<Page<QrScanAuditLogDto>> auditLogs(
            Pageable pageable,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(required = false) String gateName,
            @RequestParam(required = false) QrScanAuditLog.ScanAction action,
            @RequestParam(required = false) QrScanAuditLog.ScanStatus status,
            @RequestParam(required = false) String scannedBy,
            @RequestParam(required = false) String requestId) {
        return ApiResponse.success(
                "QR audit logs fetched",
                qrReportService.getAuditLogs(pageable, fromDate, toDate, gateName, action, status, scannedBy, requestId)
        );
    }

    @GetMapping("/movements")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ApiResponse<Page<VisitorMovementLogDto>> movementReport(
            Pageable pageable,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) String gateName,
            @RequestParam(required = false) String status) {
        return ApiResponse.success(
                "Visitor movement report fetched",
                qrReportService.getMovementReport(pageable, date, gateName, status)
        );
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

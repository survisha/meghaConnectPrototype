package com.survisha.meghaconnect.controller;

import com.survisha.meghaconnect.entity.AuditLog;
import com.survisha.meghaconnect.service.AuditLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/audit-logs")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Tag(name = "Audit Logs", description = "System audit trail and activity logs (admin, HCM, OSD only)")
@SecurityRequirement(name = "bearerAuth")
public class AuditLogController {

    private final AuditLogService auditLogService;

    @Operation(summary = "Get all audit logs", description = "Retrieve paginated audit logs (admin, HCM, OSD only)")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved audit logs",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = AuditLog.class))),
        @ApiResponse(responseCode = "403", description = "Access denied - admin role required"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','HCM','OSD')")
    public ResponseEntity<Page<AuditLog>> getAll(Pageable pageable) {
        return ResponseEntity.ok(auditLogService.getAllAuditLogs(pageable));
    }
}

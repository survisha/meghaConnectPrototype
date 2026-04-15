package com.survisha.meghaconnect.controller;

import com.survisha.meghaconnect.entity.AuditLog;
import com.survisha.meghaconnect.service.AuditLogService;
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
public class AuditLogController {

    private final AuditLogService auditLogService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','HCM','OSD')")
    public ResponseEntity<Page<AuditLog>> getAll(Pageable pageable) {
        return ResponseEntity.ok(auditLogService.getAllAuditLogs(pageable));
    }
}

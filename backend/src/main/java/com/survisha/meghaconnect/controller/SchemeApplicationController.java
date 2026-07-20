package com.survisha.meghaconnect.controller;

import com.survisha.meghaconnect.dto.CreateSchemeApplicationRequest;
import com.survisha.meghaconnect.dto.SchemeApplicationDto;
import com.survisha.meghaconnect.service.SchemeApplicationService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import javax.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/scheme-applications")
@RequiredArgsConstructor
@Tag(name = "Scheme Applications", description = "Direct CM scheme application APIs")
@SecurityRequirement(name = "bearerAuth")
@Slf4j
public class SchemeApplicationController {

    private final SchemeApplicationService schemeApplicationService;

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('PUBLIC','SUPER_ADMIN','ADMIN','OSD','DATA_ENTRY_OPERATOR')")
    public ResponseEntity<SchemeApplicationDto> create(
            @RequestBody CreateSchemeApplicationRequest request,
            @AuthenticationPrincipal UserDetails user) {
        String actor = user != null ? user.getUsername() : "anonymous";
        log.info("Scheme application create request actor={}", actor);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(schemeApplicationService.create(request, actor));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('PUBLIC','SUPER_ADMIN','ADMIN','OSD','DATA_ENTRY_OPERATOR')")
    public ResponseEntity<SchemeApplicationDto> createMultipart(
            @ModelAttribute CreateSchemeApplicationRequest form,
            HttpServletRequest request,
            @AuthenticationPrincipal UserDetails user) {
        String actor = user != null ? user.getUsername() : "anonymous";
        log.info("Scheme application multipart create request actor={}", actor);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(schemeApplicationService.createMultipart(form, request, actor));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','HCM','ADMIN','OSD','APPROVER','CMO_OFFICER')")
    public ResponseEntity<Page<SchemeApplicationDto>> getAll(
            @RequestParam(required = false) String status,
            Pageable pageable) {
        return ResponseEntity.ok(schemeApplicationService.findAll(status, pageable));
    }

    @GetMapping("/visitor/{visitorId}")
    @PreAuthorize("hasAnyRole('PUBLIC','SUPER_ADMIN','ADMIN','OSD','DATA_ENTRY_OPERATOR','HCM','APPROVER','CMO_OFFICER')")
    public ResponseEntity<List<SchemeApplicationDto>> getByVisitor(@PathVariable Long visitorId) {
        return ResponseEntity.ok(schemeApplicationService.findByVisitor(visitorId));
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','HCM','ADMIN','OSD','APPROVER','CMO_OFFICER')")
    public ResponseEntity<SchemeApplicationDto> updateStatus(
            @PathVariable Long id,
            @RequestBody Map<String, Object> body,
            @AuthenticationPrincipal UserDetails user) {
        String actor = user != null ? user.getUsername() : "system";
        String status = body != null && body.get("status") != null ? body.get("status").toString() : null;
        String remarks = body != null && body.get("remarks") != null ? body.get("remarks").toString() : null;
        java.math.BigDecimal approvedCost = body != null && body.get("hcmApprovedCost") != null
                ? new java.math.BigDecimal(body.get("hcmApprovedCost").toString())
                : null;
        return ResponseEntity.ok(schemeApplicationService.updateStatus(id, status, remarks, approvedCost, actor));
    }
}

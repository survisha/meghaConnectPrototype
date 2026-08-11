package com.survisha.meghaconnect.controller;

import com.survisha.meghaconnect.dto.*;
import com.survisha.meghaconnect.entity.DirectionFollowUp;
import com.survisha.meghaconnect.service.DirectionFollowUpService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

@RestController
@RequestMapping("/api/v1/follow-ups")
@RequiredArgsConstructor
public class DirectionFollowUpController {
    private final DirectionFollowUpService service;

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','APPROVER','HCM')")
    public DirectionFollowUpDto create(@RequestBody CreateDirectionFollowUpRequest request, Authentication auth) {
        return service.create(request, auth.getName());
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','APPROVER','HCM','DEPARTMENT_ADMIN','DEPARTMENT_PA')")
    public Page<DirectionFollowUpDto> list(@RequestParam(required = false) String status,
                                           @RequestParam(required = false) Long departmentId,
                                           @RequestParam(required = false) Boolean overdue,
                                           Pageable pageable, Authentication auth) {
        return service.find(auth.getName(), status, departmentId, overdue, pageable);
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','APPROVER','HCM','DEPARTMENT_ADMIN','DEPARTMENT_PA')")
    public DirectionFollowUpDto update(@PathVariable Long id, @RequestBody StatusRequest request, Authentication auth) {
        return service.updateStatus(id, request.getStatus(), request.getRemarks(), auth.getName());
    }

    @PostMapping(value = "/{id}/evidence", consumes = "multipart/form-data")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','APPROVER','HCM','DEPARTMENT_ADMIN','DEPARTMENT_PA')")
    public FollowUpEvidenceDto uploadEvidence(@PathVariable Long id,
                                               @RequestPart("file") MultipartFile file,
                                               @RequestParam(required = false) String documentType,
                                               Authentication auth) {
        return service.uploadEvidence(id, file, documentType, auth.getName());
    }

    @GetMapping("/{id}/evidence")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','APPROVER','HCM','DEPARTMENT_ADMIN','DEPARTMENT_PA')")
    public List<FollowUpEvidenceDto> evidence(@PathVariable Long id, Authentication auth) {
        return service.evidence(id, auth.getName());
    }

    @Data
    public static class StatusRequest {
        private DirectionFollowUp.FollowUpStatus status;
        private String remarks;
    }
}

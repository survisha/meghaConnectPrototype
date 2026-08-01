package com.survisha.meghaconnect.controller;

import com.survisha.meghaconnect.dto.DepartmentAccessRequestDto;
import com.survisha.meghaconnect.dto.CreateDepartmentAccessRequest;
import com.survisha.meghaconnect.dto.RejectDepartmentAccessRequest;
import com.survisha.meghaconnect.entity.DepartmentAccessRequest;
import com.survisha.meghaconnect.response.ApiResponse;
import com.survisha.meghaconnect.service.DepartmentAccessRequestService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import javax.validation.Valid;
import java.util.List;
import com.survisha.meghaconnect.dto.DepartmentApprovalResult;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

@RestController @RequiredArgsConstructor
@RequestMapping("/api/v1/department-access-requests")
public class DepartmentAccessRequestController {
    private final DepartmentAccessRequestService service;
    @PostMapping
    public ApiResponse<DepartmentAccessRequestDto> submit(@Valid @RequestBody CreateDepartmentAccessRequest request) {
        return ApiResponse.success("Department access request submitted", service.submit(request));
    }
    @GetMapping @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ApiResponse<Page<DepartmentAccessRequestDto>> list(
            @RequestParam(required=false) DepartmentAccessRequest.Status status,
            @RequestParam(defaultValue="0") int page,
            @RequestParam(defaultValue="20") int size) {
        int safeSize = Math.min(Math.max(size, 1), 100);
        return ApiResponse.success("Department access requests fetched",
                service.list(status, PageRequest.of(Math.max(page, 0), safeSize)));
    }
    @GetMapping("/{id}") @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ApiResponse<DepartmentAccessRequestDto> get(@PathVariable Long id) {
        return ApiResponse.success("Department access request fetched", service.get(id));
    }
    @PostMapping("/{id}/approve") @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ApiResponse<DepartmentApprovalResult> approve(@PathVariable Long id, Authentication auth) {
        return ApiResponse.success("Department request approved", service.approve(id, auth.getName()));
    }
    @PostMapping("/{id}/reject") @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ApiResponse<DepartmentAccessRequestDto> reject(@PathVariable Long id,
            @Valid @RequestBody RejectDepartmentAccessRequest request, Authentication auth) {
        return ApiResponse.success("Department request rejected", service.reject(id, request.getRejectionReason(), auth.getName()));
    }
}

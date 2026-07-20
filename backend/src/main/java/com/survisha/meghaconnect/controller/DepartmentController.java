package com.survisha.meghaconnect.controller;

import com.survisha.meghaconnect.dto.DepartmentDto;
import com.survisha.meghaconnect.dto.DepartmentRequest;
import com.survisha.meghaconnect.entity.Department;
import com.survisha.meghaconnect.response.ApiResponse;
import com.survisha.meghaconnect.service.DepartmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/v1/departments")
@RequiredArgsConstructor
@Tag(name = "Departments", description = "Department tenant management endpoints")
@SecurityRequirement(name = "bearerAuth")
public class DepartmentController {

    private final DepartmentService departmentService;

    @GetMapping
    @Operation(summary = "List departments")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ApiResponse<List<DepartmentDto>> getAll() {
        return ApiResponse.success("Departments fetched", departmentService.findAll());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get department by ID")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ApiResponse<DepartmentDto> getById(@PathVariable Long id) {
        return ApiResponse.success("Department fetched", departmentService.findById(id));
    }

    @PostMapping
    @Operation(summary = "Create department")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ApiResponse<DepartmentDto> create(@Valid @RequestBody DepartmentRequest request,
                                             Authentication authentication) {
        return ApiResponse.success("Department created", departmentService.create(request, actor(authentication)));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update department")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ApiResponse<DepartmentDto> update(@PathVariable Long id,
                                             @Valid @RequestBody DepartmentRequest request,
                                             Authentication authentication) {
        return ApiResponse.success("Department updated", departmentService.update(id, request, actor(authentication)));
    }

    @PatchMapping("/{id}/activate")
    @Operation(summary = "Activate department")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ApiResponse<DepartmentDto> activate(@PathVariable Long id, Authentication authentication) {
        return ApiResponse.success("Department activated",
                departmentService.setStatus(id, Department.DepartmentStatus.ACTIVE, actor(authentication)));
    }

    @PatchMapping("/{id}/deactivate")
    @Operation(summary = "Deactivate department")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ApiResponse<DepartmentDto> deactivate(@PathVariable Long id, Authentication authentication) {
        return ApiResponse.success("Department deactivated",
                departmentService.setStatus(id, Department.DepartmentStatus.INACTIVE, actor(authentication)));
    }

    private String actor(Authentication authentication) {
        return authentication != null ? authentication.getName() : "system";
    }
}

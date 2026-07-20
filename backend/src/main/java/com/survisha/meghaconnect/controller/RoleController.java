package com.survisha.meghaconnect.controller;

import com.survisha.meghaconnect.service.RoleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/roles")
@RequiredArgsConstructor
@Tag(name = "Roles", description = "Role lookup endpoints")
@SecurityRequirement(name = "bearerAuth")
public class RoleController {

    private final RoleService roleService;

    @Operation(summary = "Get all roles", description = "Retrieve role_name values from roles table")
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN','DEPARTMENT_ADMIN')")
    public List<String> getAllRoles() {
        return roleService.getAllRoleNames();
    }
}

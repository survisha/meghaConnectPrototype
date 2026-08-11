package com.survisha.meghaconnect.controller;

import com.survisha.meghaconnect.dto.CreateUserRequest;
import com.survisha.meghaconnect.dto.UpdateUserRequest;
import com.survisha.meghaconnect.dto.UserResponse;
import com.survisha.meghaconnect.entity.User;
import com.survisha.meghaconnect.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import javax.validation.Valid;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.domain.Sort;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(name = "Users", description = "User management endpoints")
@SecurityRequirement(name = "bearerAuth")
public class UserController {

    private final UserService userService;

    @Operation(summary = "Get all users", description = "Retrieve list of all users (admin, HCM, APPROVER only)")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved users",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = UserResponse.class))),
        @ApiResponse(responseCode = "403", description = "Access denied - admin role required"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','DEPARTMENT_ADMIN')")
    public ResponseEntity<Page<UserResponse>> getAll(
            Authentication authentication,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) User.UserRole role,
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false) Boolean locked,
            @RequestParam(required = false) Long departmentId,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(userService.getUserResponsesForActor(
                actor(authentication), search, role, active, locked, departmentId, pageable));
    }

    @Operation(summary = "Get user by ID", description = "Retrieve a specific user by their ID (admin, HCM, APPROVER only)")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved user",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = UserResponse.class))),
        @ApiResponse(responseCode = "403", description = "Access denied - admin role required"),
        @ApiResponse(responseCode = "404", description = "User not found"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','DEPARTMENT_ADMIN')")
    public ResponseEntity<UserResponse> getById(@PathVariable Long id, Authentication authentication) {
        return ResponseEntity.ok(userService.getUserResponseForActor(id, actor(authentication)));
    }

    @Operation(summary = "Create user", description = "Create an application user, including ROLE_SECURITY scanner users (admin only)")
    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','DEPARTMENT_ADMIN')")
    public com.survisha.meghaconnect.response.ApiResponse<UserResponse> create(
            @Valid @RequestBody CreateUserRequest request,
            Authentication authentication) {
        return com.survisha.meghaconnect.response.ApiResponse.success(
                "User created",
                userService.createUser(request, authentication != null ? authentication.getName() : "admin")
        );
    }

    @Operation(summary = "Update user", description = "Update a user without changing their password")
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','DEPARTMENT_ADMIN')")
    public com.survisha.meghaconnect.response.ApiResponse<UserResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserRequest request,
            Authentication authentication) {
        return com.survisha.meghaconnect.response.ApiResponse.success(
                "User updated",
                userService.updateUser(id, request, actor(authentication))
        );
    }

    @PatchMapping("/{id}/unlock")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','DEPARTMENT_ADMIN')")
    public com.survisha.meghaconnect.response.ApiResponse<UserResponse> unlock(
            @PathVariable Long id,
            Authentication authentication) {
        return com.survisha.meghaconnect.response.ApiResponse.success(
                "User unlocked successfully.",
                userService.unlockUser(id, actor(authentication))
        );
    }

    @PatchMapping("/{id}/activate")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','DEPARTMENT_ADMIN')")
    public com.survisha.meghaconnect.response.ApiResponse<UserResponse> activate(
            @PathVariable Long id,
            Authentication authentication) {
        return com.survisha.meghaconnect.response.ApiResponse.success(
                "User activated successfully.",
                userService.setActive(id, true, actor(authentication))
        );
    }

    @PatchMapping("/{id}/deactivate")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','DEPARTMENT_ADMIN')")
    public com.survisha.meghaconnect.response.ApiResponse<UserResponse> deactivate(
            @PathVariable Long id,
            Authentication authentication) {
        return com.survisha.meghaconnect.response.ApiResponse.success(
                "User deactivated successfully.",
                userService.setActive(id, false, actor(authentication))
        );
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

    private String actor(Authentication authentication) {
        return authentication != null ? authentication.getName() : "admin";
    }
}

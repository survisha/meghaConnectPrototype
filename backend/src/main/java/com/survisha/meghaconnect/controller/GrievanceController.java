package com.survisha.meghaconnect.controller;

import com.survisha.meghaconnect.entity.Grievance;
import com.survisha.meghaconnect.service.GrievanceService;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/grievances")
@RequiredArgsConstructor
@Tag(name = "Grievances", description = "Grievance management and resolution tracking")
@SecurityRequirement(name = "bearerAuth")
public class GrievanceController {

    private final GrievanceService grievanceService;

    @Operation(summary = "Get all grievances", description = "Retrieve paginated list of all grievances")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved grievances",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = Grievance.class))),
        @ApiResponse(responseCode = "403", description = "Access denied"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping
    public ResponseEntity<Page<Grievance>> getAll(Pageable pageable,
                                                  @AuthenticationPrincipal UserDetails user) {
        Long visitorId = resolveVisitorId(user);
        if (visitorId != null) {
            return ResponseEntity.ok(grievanceService.findByVisitorId(visitorId, pageable));
        }
        return ResponseEntity.ok(grievanceService.findAll(pageable));
    }

    @Operation(summary = "Get visitor grievances", description = "Retrieve grievances linked to a visitor")
    @GetMapping("/visitor/{visitorId}")
    public ResponseEntity<Page<Grievance>> getByVisitor(
            @PathVariable Long visitorId,
            Pageable pageable,
            @AuthenticationPrincipal UserDetails user) {
        Long authenticatedVisitorId = resolveVisitorId(user);
        if (authenticatedVisitorId != null && !authenticatedVisitorId.equals(visitorId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(grievanceService.findByVisitorId(visitorId, pageable));
    }

    @Operation(summary = "Get grievance by ID", description = "Retrieve a specific grievance by its ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved grievance",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = Grievance.class))),
        @ApiResponse(responseCode = "404", description = "Grievance not found"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/{id}")
    public ResponseEntity<Grievance> getById(@PathVariable Long id,
                                             @AuthenticationPrincipal UserDetails user) {
        Long visitorId = resolveVisitorId(user);
        return (visitorId != null ? grievanceService.findByIdForVisitor(id, visitorId) : grievanceService.findById(id))
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Create grievance", description = "Create a new grievance")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Grievance created successfully",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = Grievance.class))),
        @ApiResponse(responseCode = "400", description = "Invalid grievance data"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Grievance create(@RequestBody Grievance grievance,
                            @AuthenticationPrincipal UserDetails user) {
        String actor = user != null ? user.getUsername() : "anonymous";
        Long visitorId = resolveVisitorId(user);
        if (visitorId != null) {
            return grievanceService.createForVisitor(grievance, visitorId, actor);
        }
        return grievanceService.create(grievance, actor);
    }

    @Operation(summary = "Update grievance", description = "Update grievance details")
    @PutMapping("/{id}")
    public ResponseEntity<Grievance> update(
            @PathVariable Long id,
            @RequestBody Grievance grievance,
            @AuthenticationPrincipal UserDetails user) {
        String actor = user != null ? user.getUsername() : "anonymous";
        Long visitorId = resolveVisitorId(user);
        if (visitorId != null) {
            return ResponseEntity.ok(grievanceService.updateForVisitor(id, visitorId, grievance, actor));
        }
        return ResponseEntity.ok(grievanceService.update(id, grievance, actor));
    }

    @Operation(summary = "Delete grievance", description = "Delete a grievance")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails user) {
        String actor = user != null ? user.getUsername() : "anonymous";
        Long visitorId = resolveVisitorId(user);
        if (visitorId != null) {
            grievanceService.deleteForVisitor(id, visitorId, actor);
        } else {
            grievanceService.delete(id, actor);
        }
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Update grievance status", description = "Update the status of a grievance")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Grievance status updated successfully",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = Grievance.class))),
        @ApiResponse(responseCode = "400", description = "Invalid status value"),
        @ApiResponse(responseCode = "404", description = "Grievance not found"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PatchMapping("/{id}/status")
    public ResponseEntity<?> updateStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal UserDetails user) {
        if (resolveVisitorId(user) != null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        String actor = user != null ? user.getUsername() : "anonymous";
        return ResponseEntity.ok(grievanceService.updateStatus(id, body, actor));
    }

    private Long resolveVisitorId(UserDetails user) {
        if (user == null || user.getUsername() == null || !user.getUsername().startsWith("visitor_")) {
            return null;
        }
        try {
            return Long.parseLong(user.getUsername().substring("visitor_".length()));
        } catch (NumberFormatException e) {
            return null;
        }
    }
}

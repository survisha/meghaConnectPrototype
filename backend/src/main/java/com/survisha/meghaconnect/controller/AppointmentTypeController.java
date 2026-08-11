package com.survisha.meghaconnect.controller;

import com.survisha.meghaconnect.dto.AppointmentTypeDto;
import com.survisha.meghaconnect.service.AppointmentTypeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/admin/appointment-types")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Appointment Types", description = "Appointment type configuration management (admin only)")
@SecurityRequirement(name = "bearerAuth")
@org.springframework.security.access.prepost.PreAuthorize("@accessPolicy.canManageCmoConfiguration()")
public class AppointmentTypeController {

    private final AppointmentTypeService appointmentTypeService;

    /**
     * Get all appointment type configurations
     */
    @Operation(summary = "Get all appointment types", description = "Retrieve all appointment type configurations")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved appointment types",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = AppointmentTypeDto.class))),
        @ApiResponse(responseCode = "403", description = "Access denied - admin role required"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping
    public ResponseEntity<?> getAllAppointmentTypes(HttpServletRequest request) {
        try {
            if (!appointmentTypeService.isAdminUser(request)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("Access denied. Admin role required.");
            }

            List<AppointmentTypeDto> types = appointmentTypeService.getAllAppointmentTypes();
            return ResponseEntity.ok(types);
        } catch (Exception e) {
            log.error("Error fetching appointment types", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error fetching appointment types");
        }
    }

    /**
     * Get appointment type by code
     */
    @Operation(summary = "Get appointment type by code", description = "Retrieve a specific appointment type by its code")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved appointment type",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = AppointmentTypeDto.class))),
        @ApiResponse(responseCode = "403", description = "Access denied - admin role required"),
        @ApiResponse(responseCode = "404", description = "Appointment type not found"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/{typeCode}")
    public ResponseEntity<?> getAppointmentTypeByCode(@PathVariable String typeCode, HttpServletRequest request) {
        try {
            if (!appointmentTypeService.isAdminUser(request)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("Access denied. Admin role required.");
            }

            Optional<AppointmentTypeDto> appointmentType = appointmentTypeService.getAppointmentTypeByCode(typeCode);
            if (appointmentType.isPresent()) {
                return ResponseEntity.ok(appointmentType.get());
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Appointment type not found");
            }
        } catch (Exception e) {
            log.error("Error fetching appointment type", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error fetching appointment type");
        }
    }

    /**
     * Get appointment types by category
     */
    @Operation(summary = "Get appointment types by category", description = "Retrieve appointment types filtered by category")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved appointment types",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = AppointmentTypeDto.class))),
        @ApiResponse(responseCode = "403", description = "Access denied - admin role required"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/category/{category}")
    public ResponseEntity<?> getByCategory(@PathVariable String category, HttpServletRequest request) {
        try {
            if (!appointmentTypeService.isAdminUser(request)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("Access denied. Admin role required.");
            }

            List<AppointmentTypeDto> types = appointmentTypeService.getAppointmentTypesByCategory(category);
            return ResponseEntity.ok(types);
        } catch (Exception e) {
            log.error("Error fetching appointment types by category", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error fetching appointment types");
        }
    }

    /**
     * Create new appointment type configuration
     */
    @Operation(summary = "Create appointment type", description = "Create a new appointment type configuration")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Appointment type created successfully",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = AppointmentTypeDto.class))),
        @ApiResponse(responseCode = "400", description = "Invalid appointment type input"),
        @ApiResponse(responseCode = "403", description = "Access denied - admin role required"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping
    public ResponseEntity<?> createAppointmentType(@RequestBody AppointmentTypeDto dto, HttpServletRequest request) {
        try {
            if (!appointmentTypeService.isAdminUser(request)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("Access denied. Admin role required.");
            }

            AppointmentTypeDto created = appointmentTypeService.createAppointmentType(dto, request);
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid appointment type input: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            log.error("Error creating appointment type", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error creating appointment type");
        }
    }

    /**
     * Update appointment type configuration
     */
    @Operation(summary = "Update appointment type", description = "Update an existing appointment type configuration")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Appointment type updated successfully",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = AppointmentTypeDto.class))),
        @ApiResponse(responseCode = "400", description = "Invalid appointment type input"),
        @ApiResponse(responseCode = "403", description = "Access denied - admin role required"),
        @ApiResponse(responseCode = "404", description = "Appointment type not found"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PutMapping("/{typeCode}")
    public ResponseEntity<?> updateAppointmentType(@PathVariable String typeCode, 
                                                   @RequestBody AppointmentTypeDto dto,
                                                   HttpServletRequest request) {
        try {
            if (!appointmentTypeService.isAdminUser(request)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("Access denied. Admin role required.");
            }

            AppointmentTypeDto updated = appointmentTypeService.updateAppointmentType(typeCode, dto, request);
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid appointment type: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (Exception e) {
            log.error("Error updating appointment type", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error updating appointment type");
        }
    }

    /**
     * Toggle active/inactive status of appointment type
     */
    @Operation(summary = "Toggle appointment type status", description = "Toggle the active/inactive status of an appointment type")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Appointment type status toggled successfully",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = AppointmentTypeDto.class))),
        @ApiResponse(responseCode = "403", description = "Access denied - admin role required"),
        @ApiResponse(responseCode = "404", description = "Appointment type not found"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PatchMapping("/{typeCode}/toggle")
    public ResponseEntity<?> toggleStatus(@PathVariable String typeCode, HttpServletRequest request) {
        try {
            if (!appointmentTypeService.isAdminUser(request)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("Access denied. Admin role required.");
            }

            AppointmentTypeDto updated = appointmentTypeService.toggleAppointmentTypeStatus(typeCode, request);
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            log.warn("Appointment type not found: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (Exception e) {
            log.error("Error toggling appointment type status", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error toggling status");
        }
    }
}

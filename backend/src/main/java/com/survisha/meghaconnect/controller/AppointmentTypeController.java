package com.survisha.meghaconnect.controller;

import com.survisha.meghaconnect.dto.AppointmentTypeDto;
import com.survisha.meghaconnect.service.AppointmentTypeService;
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
public class AppointmentTypeController {

    private final AppointmentTypeService appointmentTypeService;

    /**
     * Get all appointment type configurations
     */
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

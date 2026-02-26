package gov.meghalaya.meghaconnect.controller;

import gov.meghalaya.meghaconnect.dto.AppointmentDto;
import gov.meghalaya.meghaconnect.entity.Appointment;
import gov.meghalaya.meghaconnect.service.AppointmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/appointments")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AppointmentController {

    private final AppointmentService appointmentService;

    @GetMapping
    public ResponseEntity<Page<Appointment>> getAll(Pageable pageable) {
        return ResponseEntity.ok(appointmentService.findAll(pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Appointment> getById(@PathVariable Long id) {
        return appointmentService.findById(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/by-app-id/{appId}")
    public ResponseEntity<Appointment> getByApplicationId(@PathVariable String appId) {
        return appointmentService.findByApplicationId(appId)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Appointment create(@Valid @RequestBody AppointmentDto dto,
                              @AuthenticationPrincipal UserDetails user) {
        return appointmentService.create(dto, user.getUsername());
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('CMO_OFFICER','APPROVER_JT_SECY','HCM','SAIDUL_OSD','ADMIN')")
    public ResponseEntity<?> updateStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal UserDetails user) {
        String statusStr = body.get("status");
        if (statusStr == null || statusStr.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "'status' field is required"));
        }
        try {
            Appointment.AppointmentStatus status = Appointment.AppointmentStatus.valueOf(statusStr);
            return ResponseEntity.ok(
                appointmentService.updateStatus(id, status, body.get("remarks"), user.getUsername())
            );
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid status value: " + statusStr));
        }
    }

    @PostMapping("/{id}/schedule")
    @PreAuthorize("hasAnyRole('CMO_OFFICER','APPROVER_JT_SECY','HCM','SAIDUL_OSD','ADMIN')")
    public ResponseEntity<?> schedule(
            @PathVariable Long id,
            @RequestBody Map<String, Object> body,
            @AuthenticationPrincipal UserDetails user) {

        Object dtObj = body.get("scheduledDateTime");
        Object durObj = body.get("durationMinutes");

        if (dtObj == null || durObj == null) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "'scheduledDateTime' and 'durationMinutes' are required"));
        }

        if (!(durObj instanceof Integer)) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "'durationMinutes' must be an integer"));
        }

        try {
            LocalDateTime dt = LocalDateTime.parse(dtObj.toString());
            int duration = (Integer) durObj;
            return ResponseEntity.ok(appointmentService.schedule(id, dt, duration, user.getUsername()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid date-time format: " + dtObj));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("error", e.getMessage()));
        }
    }
}

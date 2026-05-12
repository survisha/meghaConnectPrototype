package com.survisha.meghaconnect.controller;

import com.survisha.meghaconnect.dto.ScheduleEventAppointmentAssignmentRequest;
import com.survisha.meghaconnect.dto.ScheduleEventDto;
import com.survisha.meghaconnect.entity.ScheduleEvent;
import com.survisha.meghaconnect.service.ScheduleEventService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/schedule")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Tag(name = "Schedule Events", description = "Schedule event management and calendar operations")
@SecurityRequirement(name = "bearerAuth")
public class ScheduleEventController {

    private final ScheduleEventService scheduleEventService;

    @GetMapping
    public List<ScheduleEventDto> getAll(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {
        return scheduleEventService.findAllDtos(start, end);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ScheduleEventDto> getById(@PathVariable Long id) {
        return scheduleEventService.findDtoById(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('CMO_OFFICER','APPROVER','HCM','OSD','ADMIN')")
    public ScheduleEventDto create(@RequestBody ScheduleEvent event) {
        return scheduleEventService.toDto(scheduleEventService.create(event));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('CMO_OFFICER','APPROVER','HCM','OSD','ADMIN')")
    public ScheduleEventDto update(@PathVariable Long id, @RequestBody ScheduleEvent event) {
        event.setId(id);
        return scheduleEventService.toDto(scheduleEventService.update(event));
    }

    @PostMapping("/{id}/appointments")
    @PreAuthorize("hasAnyRole('CMO_OFFICER','APPROVER','HCM','OSD','ADMIN')")
    public ScheduleEventDto assignAppointments(@PathVariable Long id,
                                               @RequestBody ScheduleEventAppointmentAssignmentRequest request,
                                               Authentication authentication) {
        return scheduleEventService.assignAppointments(
            id,
            request,
            actor(authentication),
            role(authentication)
        );
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('CMO_OFFICER','APPROVER','HCM','OSD','ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        scheduleEventService.delete(id);
        return ResponseEntity.noContent().build();
    }

    private String actor(Authentication authentication) {
        return authentication != null ? authentication.getName() : "anonymous";
    }

    private String role(Authentication authentication) {
        if (authentication == null || authentication.getAuthorities() == null) {
            return "ANONYMOUS";
        }
        return authentication.getAuthorities()
            .stream()
            .map(GrantedAuthority::getAuthority)
            .findFirst()
            .map(authority -> authority.replace("ROLE_", ""))
            .orElse("ANONYMOUS");
    }
}

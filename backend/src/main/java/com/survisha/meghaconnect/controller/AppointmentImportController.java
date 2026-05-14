package com.survisha.meghaconnect.controller;

import com.survisha.meghaconnect.dto.PilotImportResultDto;
import com.survisha.meghaconnect.service.PilotAppointmentImportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/appointments")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Tag(name = "Appointment Imports", description = "Bulk appointment import tools")
@SecurityRequirement(name = "bearerAuth")
@Slf4j
public class AppointmentImportController {

    private final PilotAppointmentImportService pilotAppointmentImportService;

    @Operation(summary = "Import pilot appointments from Excel")
    @PostMapping(value = "/pilot-import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN','OSD','DATA_ENTRY_OPERATOR')")
    public ResponseEntity<PilotImportResultDto> importPilotAppointments(
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal UserDetails user) {
        String actor = user != null ? user.getUsername() : "pilot-import";
        log.info("Pilot appointment import requested by={}", actor);
        return ResponseEntity.ok(pilotAppointmentImportService.importPilotSheet(file, actor));
    }
}

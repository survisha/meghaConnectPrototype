package com.survisha.meghaconnect.controller;

import com.survisha.meghaconnect.dto.GuestAppointmentRequest;
import com.survisha.meghaconnect.dto.GuestAppointmentResponse;
import com.survisha.meghaconnect.service.AppointmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/guest-appointments")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Tag(name = "Guest Appointments", description = "Public guest appointment submission")
public class GuestAppointmentController {

    private final AppointmentService appointmentService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Submit guest appointment request")
    public ResponseEntity<GuestAppointmentResponse> createMultipart(@ModelAttribute GuestAppointmentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(appointmentService.createGuestAppointment(request));
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Submit guest appointment request")
    public ResponseEntity<GuestAppointmentResponse> createJson(@RequestBody GuestAppointmentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(appointmentService.createGuestAppointment(request));
    }
}

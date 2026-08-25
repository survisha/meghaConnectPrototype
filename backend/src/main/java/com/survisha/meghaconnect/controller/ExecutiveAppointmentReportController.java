package com.survisha.meghaconnect.controller;

import com.survisha.meghaconnect.dto.*;
import com.survisha.meghaconnect.service.ExecutiveAppointmentReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('SUPER_ADMIN','DEO','APPROVER','HCM')")
public class ExecutiveAppointmentReportController {
    private final ExecutiveAppointmentReportService service;

    @GetMapping("/completed-appointments")
    public Page<CompletedAppointmentSummaryResponse> completed(@ModelAttribute ExecutiveAppointmentReportFilter filter, Pageable pageable) {
        return service.completed(filter, pageable);
    }

    @GetMapping("/completed-appointments/{id}")
    public CompletedAppointmentDetailResponse completedDetail(@PathVariable Long id, Authentication auth) {
        return service.completedDetail(id, auth.getName());
    }

    @GetMapping(value = "/completed-appointments/{id}/photo", produces = {MediaType.IMAGE_JPEG_VALUE, MediaType.IMAGE_PNG_VALUE})
    public ResponseEntity<byte[]> photo(@PathVariable Long id) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(service.photo(id));
    }

    @GetMapping(value = "/completed-appointments/{id}/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> completedPdf(@PathVariable Long id, Authentication auth) {
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=completed-appointment-" + id + ".pdf")
                .body(service.completedPdf(id, auth.getName()));
    }

    @GetMapping(value = "/completed-appointments/export", produces = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    public ResponseEntity<byte[]> completedExcel(@ModelAttribute ExecutiveAppointmentReportFilter filter, Authentication auth) {
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=completed-appointments.xlsx")
                .body(service.completedExcel(filter, auth.getName()));
    }

    @GetMapping("/rejected-appointments")
    public Page<RejectedAppointmentSummaryResponse> rejected(@ModelAttribute ExecutiveAppointmentReportFilter filter, Pageable pageable) {
        return service.rejected(filter, pageable);
    }

    @GetMapping("/rejected-appointments/{id}")
    public RejectedAppointmentDetailResponse rejectedDetail(@PathVariable Long id, Authentication auth) {
        return service.rejectedDetail(id, auth.getName());
    }
}

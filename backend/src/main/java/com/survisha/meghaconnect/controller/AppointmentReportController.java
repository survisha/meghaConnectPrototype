package com.survisha.meghaconnect.controller;

import com.survisha.meghaconnect.dto.*;
import com.survisha.meghaconnect.service.AppointmentReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/reports/appointments")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('SUPER_ADMIN','APPROVER','HCM','DEPARTMENT_ADMIN','DEPARTMENT_PA')")
public class AppointmentReportController {
    private final AppointmentReportService service;

    @GetMapping
    public Page<AppointmentReportRow> search(@ModelAttribute AppointmentReportFilter filter,
                                             Pageable pageable, Authentication auth) {
        return service.search(filter, pageable, auth.getName());
    }

    @GetMapping(value = "/export.xlsx", produces = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    public ResponseEntity<byte[]> excel(@ModelAttribute AppointmentReportFilter filter, Authentication auth) {
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=meghaconnect-appointments.xlsx")
                .body(service.exportExcel(filter, auth.getName()));
    }

    @GetMapping(value = "/export.pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> pdf(@ModelAttribute AppointmentReportFilter filter, Authentication auth) {
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=meghaconnect-appointments.pdf")
                .body(service.exportPdf(filter, auth.getName()));
    }
}

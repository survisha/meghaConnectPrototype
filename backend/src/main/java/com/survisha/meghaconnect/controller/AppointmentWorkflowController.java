package com.survisha.meghaconnect.controller;

import com.survisha.meghaconnect.dto.*;
import com.survisha.meghaconnect.entity.Appointment;
import com.survisha.meghaconnect.response.ApiResponse;
import com.survisha.meghaconnect.service.AppointmentWorkflowService;
import com.survisha.meghaconnect.service.PublicDarbarSchedulingService;
import com.survisha.meghaconnect.service.PublicDarbarService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/appointments")
@RequiredArgsConstructor
@Slf4j
public class AppointmentWorkflowController {

    private final AppointmentWorkflowService appointmentWorkflowService;
    private final PublicDarbarService publicDarbarService;
    private final PublicDarbarSchedulingService publicDarbarSchedulingService;

    @PostMapping("/citizen")
    @PreAuthorize("hasAnyRole('PUBLIC','CITIZEN')")
    public ApiResponse<AppointmentWorkflowResponse> createCitizenAppointment(@Valid @RequestBody CitizenAppointmentRequest request,
                                                                             Authentication authentication) {
        log.info("Citizen appointment create request received");
        return ApiResponse.success(
                "Appointment draft created",
                appointmentWorkflowService.createDraft(
                        request,
                        visitorId(authentication),
                        actor(authentication),
                        role(authentication)
                )
        );
    }

    @PutMapping("/citizen/{id}")
    @PreAuthorize("hasAnyRole('PUBLIC','CITIZEN')")
    public ApiResponse<AppointmentWorkflowResponse> updateCitizenAppointment(@PathVariable Long id,
                                                                             @Valid @RequestBody CitizenAppointmentRequest request,
                                                                             Authentication authentication) {
        log.info("Citizen appointment update request received appointmentId={}", id);
        return ApiResponse.success(
                "Appointment draft updated",
                appointmentWorkflowService.updateDraft(id, request, visitorId(authentication), actor(authentication), role(authentication))
        );
    }

    @PostMapping("/citizen/{id}/submit")
    @PreAuthorize("hasAnyRole('PUBLIC','CITIZEN')")
    public ApiResponse<AppointmentWorkflowResponse> submitCitizenAppointment(@PathVariable Long id,
                                                                             Authentication authentication) {
        log.info("Citizen appointment submit request received appointmentId={}", id);
        return ApiResponse.success(
                "Appointment submitted for approver review",
                appointmentWorkflowService.submit(id, visitorId(authentication), actor(authentication), role(authentication))
        );
    }

    @GetMapping("/citizen")
    @PreAuthorize("hasAnyRole('PUBLIC','CITIZEN')")
    public ApiResponse<List<AppointmentWorkflowResponse>> getCitizenAppointments(@RequestParam(required = false) Long applicantId,
                                                                                 Authentication authentication) {
        log.info("Citizen appointment list request received");
        return ApiResponse.success(
                "Appointments fetched",
                appointmentWorkflowService.getCitizenAppointments(visitorId(authentication), applicantId, role(authentication))
        );
    }

    @GetMapping("/citizen/{id}")
    @PreAuthorize("hasAnyRole('PUBLIC','CITIZEN')")
    public ApiResponse<AppointmentWorkflowResponse> getCitizenAppointment(@PathVariable Long id,
                                                                          Authentication authentication) {
        log.info("Citizen appointment detail request received appointmentId={}", id);
        return ApiResponse.success(
                "Appointment fetched",
                appointmentWorkflowService.getCitizenAppointment(id, visitorId(authentication), role(authentication))
        );
    }

    @GetMapping("/approver/pending")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','CMO','CMO_OFFICER','OSD','APPROVER')")
    public ApiResponse<List<AppointmentWorkflowResponse>> getPendingForApprover(
            @RequestParam(required = false) String district,
            @RequestParam(required = false) String department,
            @RequestParam(required = false) Appointment.AppointmentStatus status,
            @RequestParam(required = false) String appointmentType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(required = false) String search,
            Authentication authentication) {
        log.info("Approver pending appointment filter request received");
        return ApiResponse.success(
                "Pending appointments fetched",
                appointmentWorkflowService.findPendingForApprover(
                        district,
                        department,
                        status,
                        appointmentType,
                        fromDate,
                        toDate,
                        search,
                        role(authentication)
                )
        );
    }

    @PostMapping("/approver/{id}/select-public-darbar")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','CMO','CMO_OFFICER','OSD','APPROVER')")
    public ApiResponse<AppointmentWorkflowResponse> markForPublicDarbar(@PathVariable Long id,
                                                                        @RequestBody(required = false) MarkPublicDarbarRequest request,
                                                                        Authentication authentication) {
        log.info("Approver select Public Darbar request received appointmentId={}", id);
        return ApiResponse.success(
                "Appointment marked for follow-up",
                appointmentWorkflowService.markFollowUp(id, request, actor(authentication), role(authentication))
        );
    }

    @PostMapping("/approver/{id}/approve")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','CMO','CMO_OFFICER','OSD','APPROVER')")
    public ApiResponse<AppointmentWorkflowResponse> approveNormalAppointment(@PathVariable Long id,
                                                                             @Valid @RequestBody ApproveAppointmentRequest request,
                                                                             Authentication authentication) {
        log.info("Approver normal approval request received appointmentId={}", id);
        return ApiResponse.success(
                "Appointment approved with scheduled date/time",
                appointmentWorkflowService.approveWithDateTime(id, request, actor(authentication), role(authentication))
        );
    }

    @PostMapping("/approver/{id}/reject")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','CMO','CMO_OFFICER','OSD','APPROVER')")
    public ApiResponse<AppointmentWorkflowResponse> rejectAppointment(@PathVariable Long id,
                                                                      @Valid @RequestBody RejectAppointmentRequest request,
                                                                      Authentication authentication) {
        log.info("Approver rejection request received appointmentId={}", id);
        return ApiResponse.success(
                "Appointment rejected",
                appointmentWorkflowService.reject(id, request, actor(authentication), role(authentication))
        );
    }

    @GetMapping("/approver/public-darbar-selected")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','CMO','CMO_OFFICER','OSD','APPROVER')")
    public ApiResponse<List<AppointmentWorkflowResponse>> getSelectedForPublicDarbar(Authentication authentication) {
        log.info("Approver selected Public Darbar appointment list request received");
        return ApiResponse.success(
                "Selected Public Darbar appointments fetched",
                appointmentWorkflowService.getSelectedPublicDarbarAppointments(role(authentication))
        );
    }

    @PostMapping("/admin/public-darbars")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public ApiResponse<PublicDarbarResponse> createPublicDarbar(@Valid @RequestBody PublicDarbarRequest request,
                                                                Authentication authentication) {
        log.info("Admin Public Darbar create request received");
        return ApiResponse.success(
                "Public Darbar date created",
                publicDarbarService.create(request, actor(authentication))
        );
    }

    @PostMapping("/admin/public-darbars/{id}/activate")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public ApiResponse<Map<String, Object>> activatePublicDarbar(@PathVariable Long id,
                                                                  Authentication authentication) {
        log.info("Admin Public Darbar activate request received publicDarbarId={}", id);
        PublicDarbarResponse darbar = publicDarbarService.activate(id, actor(authentication));
        BatchScheduleResult scheduleResult = publicDarbarSchedulingService.scheduleSelectedForDarbar(
                id,
                actor(authentication),
                role(authentication),
                null,
                false
        );
        Map<String, Object> response = new HashMap<>();
        response.put("publicDarbar", darbar);
        response.put("scheduleResult", scheduleResult);
        return ApiResponse.success("Public Darbar date activated", response);
    }

    @PostMapping("/admin/public-darbars/{id}/schedule")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public ApiResponse<BatchScheduleResult> schedulePublicDarbarAppointments(@PathVariable Long id,
                                                                             Authentication authentication) {
        log.info("Admin manual Public Darbar scheduling request received publicDarbarId={}", id);
        return ApiResponse.success(
                "Public Darbar scheduling completed",
                publicDarbarSchedulingService.scheduleSelectedForDarbar(
                        id,
                        actor(authentication),
                        role(authentication),
                        null,
                        true
                )
        );
    }

    @GetMapping("/admin/public-darbars/{id}/appointments")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public ApiResponse<List<AppointmentWorkflowResponse>> getPublicDarbarAppointments(@PathVariable Long id) {
        log.info("Admin Public Darbar appointment list request received publicDarbarId={}", id);
        return ApiResponse.success(
                "Public Darbar appointments fetched",
                appointmentWorkflowService.getPublicDarbarAppointments(id)
        );
    }

    @PutMapping("/admin/public-darbars/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public ApiResponse<PublicDarbarResponse> reschedulePublicDarbar(@PathVariable Long id,
                                                                    @Valid @RequestBody PublicDarbarRequest request,
                                                                    Authentication authentication) {
        log.info("Admin Public Darbar reschedule request received publicDarbarId={}", id);
        return ApiResponse.success(
                "Public Darbar date updated",
                publicDarbarService.reschedule(id, request, actor(authentication))
        );
    }

    @PostMapping("/admin/public-darbars/{id}/cancel")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public ApiResponse<PublicDarbarResponse> cancelPublicDarbar(@PathVariable Long id,
                                                                @RequestBody(required = false) Map<String, String> request,
                                                                Authentication authentication) {
        log.info("Admin Public Darbar cancel request received publicDarbarId={}", id);
        String remarks = request != null ? request.get("remarks") : null;
        return ApiResponse.success(
                "Public Darbar date cancelled",
                publicDarbarService.cancel(id, actor(authentication), remarks)
        );
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

    private Long visitorId(Authentication authentication) {
        String actor = actor(authentication);
        if (!actor.startsWith("visitor_")) {
            return null;
        }
        try {
            return Long.parseLong(actor.substring("visitor_".length()));
        } catch (Exception e) {
            return null;
        }
    }
}

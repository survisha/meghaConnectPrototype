package com.survisha.meghaconnect.controller;

import com.survisha.meghaconnect.config.RequestIdResponseAdvice;
import com.survisha.meghaconnect.entity.Appointment;
import com.survisha.meghaconnect.service.AppointmentDocumentAiNotesService;
import com.survisha.meghaconnect.service.AppointmentService;
import com.survisha.meghaconnect.service.AuditLogService;
import com.survisha.meghaconnect.service.HcmActionService;
import com.survisha.meghaconnect.service.ScheduleEventService;
import com.survisha.meghaconnect.service.VisitorPassService;
import com.survisha.meghaconnect.service.WalkInTokenService;
import com.survisha.meghaconnect.util.RequestContextUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class WalkInDashboardResponseTest {

    private static final String REQUEST_ID = "2b0e39f0-49e8-4e6b-87a2-ef3b0f1e8a90";

    @Mock private AppointmentService appointmentService;
    @Mock private AppointmentDocumentAiNotesService appointmentDocumentAiNotesService;
    @Mock private ScheduleEventService scheduleEventService;
    @Mock private HcmActionService hcmActionService;
    @Mock private VisitorPassService visitorPassService;
    @Mock private AuditLogService auditLogService;
    @Mock private WalkInTokenService walkInTokenService;
    @Mock private Authentication authentication;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        RequestContextUtil.setRequestId(REQUEST_ID);
        AppointmentController controller = new AppointmentController(
                appointmentService,
                appointmentDocumentAiNotesService,
                scheduleEventService,
                hcmActionService,
                visitorPassService,
                auditLogService,
                walkInTokenService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new RequestIdResponseAdvice())
                .build();
        when(authentication.getName()).thenReturn("approver");
    }

    @AfterEach
    void tearDown() {
        RequestContextUtil.clear();
    }

    @Test
    void serializesNumericCountsAndStringRequestIdWithoutLongCast() throws Exception {
        when(appointmentService.countWalkIns(Appointment.AppointmentStatus.PENDING, "approver"))
                .thenReturn(18L);
        when(appointmentService.countWalkIns(Appointment.AppointmentStatus.COMPLETED, "approver"))
                .thenReturn(35L);

        mockMvc.perform(get("/api/v1/appointments/dashboard/walk-ins")
                        .principal(authentication))
                .andExpect(status().isOk())
                .andExpect(header().string(RequestContextUtil.REQUEST_ID_HEADER, REQUEST_ID))
                .andExpect(jsonPath("$.walkInPending").value(18))
                .andExpect(jsonPath("$.walkInCompleted").value(35))
                .andExpect(jsonPath("$.requestId").value(REQUEST_ID));
    }

    @Test
    void serializesZeroCountsWithStringRequestId() throws Exception {
        when(appointmentService.countWalkIns(Appointment.AppointmentStatus.PENDING, "approver"))
                .thenReturn(0L);
        when(appointmentService.countWalkIns(Appointment.AppointmentStatus.COMPLETED, "approver"))
                .thenReturn(0L);

        mockMvc.perform(get("/api/v1/appointments/dashboard/walk-ins")
                        .principal(authentication))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.walkInPending").value(0))
                .andExpect(jsonPath("$.walkInCompleted").value(0))
                .andExpect(jsonPath("$.requestId").isString());
    }
}

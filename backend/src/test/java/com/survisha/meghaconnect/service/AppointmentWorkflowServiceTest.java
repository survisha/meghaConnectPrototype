package com.survisha.meghaconnect.service;

import com.survisha.meghaconnect.dto.MarkPublicDarbarRequest;
import com.survisha.meghaconnect.dto.RejectAppointmentRequest;
import com.survisha.meghaconnect.entity.Appointment;
import com.survisha.meghaconnect.entity.Visitor;
import com.survisha.meghaconnect.exception.ErrorCodeConstants;
import com.survisha.meghaconnect.exception.MeghaConnectException;
import com.survisha.meghaconnect.repository.AppointmentRepository;
import com.survisha.meghaconnect.repository.VisitorRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AppointmentWorkflowServiceTest {

    @Mock
    private AppointmentRepository appointmentRepository;

    @Mock
    private VisitorRepository visitorRepository;

    @Mock
    private AppointmentAuditService appointmentAuditService;

    @Mock
    private AppointmentNotificationService notificationService;

    @Mock
    private QrTokenService qrTokenService;

    @InjectMocks
    private AppointmentWorkflowService appointmentWorkflowService;

    @Test
    void markForPublicDarbarMovesB1AppointmentToFollowupAndAudits() {
        Appointment appointment = Appointment.builder()
                .applicationId("MC-2026-000001")
                .applicant(Visitor.builder().id(7L).fullName("Citizen").phoneNumber("9876543210").build())
                .eventType(Appointment.EventType.B1)
                .status(Appointment.AppointmentStatus.PENDING_APPROVER_REVIEW)
                .build();
        appointment.setId(42L);

        when(appointmentRepository.findById(42L)).thenReturn(Optional.of(appointment));
        when(appointmentRepository.save(any(Appointment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        appointmentWorkflowService.markForPublicDarbar(
                42L,
                MarkPublicDarbarRequest.builder().remarks("Suitable for Public Darbar").build(),
                "cmo",
                "CMO_OFFICER"
        );

        assertEquals(Appointment.AppointmentStatus.FOLLOWUP, appointment.getStatus());
        assertEquals("cmo", appointment.getSelectedForPublicDarbarBy());
        verify(appointmentAuditService).recordStatusChange(
                eq(appointment),
                eq(Appointment.AppointmentStatus.PENDING_APPROVER_REVIEW),
                eq(Appointment.AppointmentStatus.FOLLOWUP),
                eq("FOLLOWUP"),
                any(),
                eq("cmo"),
                eq("CMO_OFFICER")
        );
        verify(notificationService).appointmentSelectedForPublicDarbar(appointment);
    }

    @Test
    void rejectRequiresReason() {
        MeghaConnectException exception = assertThrows(
                MeghaConnectException.class,
                () -> appointmentWorkflowService.reject(
                        42L,
                        RejectAppointmentRequest.builder().reason(" ").build(),
                        "osd",
                        "OSD"
                )
        );

        assertEquals(ErrorCodeConstants.APPT_REJECTION_REASON_REQUIRED, exception.getErrorCode());
        verifyNoInteractions(appointmentRepository);
    }
}

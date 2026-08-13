package com.survisha.meghaconnect.service;

import com.survisha.meghaconnect.dto.HcmActionDto;
import com.survisha.meghaconnect.entity.Appointment;
import com.survisha.meghaconnect.entity.HcmAction;
import com.survisha.meghaconnect.repository.AppointmentRepository;
import com.survisha.meghaconnect.repository.HcmActionRepository;
import com.survisha.meghaconnect.repository.ReferenceDataRepository;
import com.survisha.meghaconnect.security.JwtUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class HcmActionServiceRemarkTest {
    private final HcmActionRepository actions = mock(HcmActionRepository.class);
    private final AppointmentRepository appointments = mock(AppointmentRepository.class);
    private final ReferenceDataRepository referenceData = mock(ReferenceDataRepository.class);
    private final AuditLogService audit = mock(AuditLogService.class);
    private final HcmActionService service = new HcmActionService(actions, appointments, referenceData, audit, mock(JwtUtils.class));
    private Appointment appointment;

    @BeforeEach
    void setUp() {
        appointment = Appointment.builder()
                .id(10L)
                .status(Appointment.AppointmentStatus.SCHEDULED)
                .scheduledDateTime(LocalDateTime.now())
                .approverRemarks("Approver original")
                .hcmRemarks("HCM original")
                .build();
        when(appointments.findById(10L)).thenReturn(Optional.of(appointment));
        when(actions.save(any(HcmAction.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void approverCanRecordHcmRemarkWithoutOverwritingApproverRemarkAndIsAuditedAsActor() {
        service.addRemark(10L, HcmActionDto.builder()
                .decision("HCM_REMARK").hcmRemarks("HCM direction recorded by Approver").build(),
                "approver01", "APPROVER");

        assertEquals("Approver original", appointment.getApproverRemarks());
        assertEquals("HCM direction recorded by Approver", appointment.getHcmRemarks());
        verify(audit).log("Appointment", 10L, "REMARK_ADDED", "APPROVER remarks added", "approver01");
    }

    @Test
    void hcmCannotOverwriteApproverRemark() {
        assertThrows(IllegalArgumentException.class, () -> service.addRemark(10L,
                HcmActionDto.builder().decision("APPROVER_REMARK").hcmRemarks("Not allowed").build(),
                "hcm01", "HCM"));
        assertEquals("Approver original", appointment.getApproverRemarks());
    }
}

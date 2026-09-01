package com.survisha.meghaconnect.service;

import com.survisha.meghaconnect.entity.Appointment;
import com.survisha.meghaconnect.repository.AppointmentRepository;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AiDashboardDummyStatusRegressionTest {

    @Test
    void dashboardUsesSqlFilteredReadsAndKeepsDummyOutsideBusinessEnum() {
        AppointmentRepository appointments = mock(AppointmentRepository.class);
        AiClientService aiClient = mock(AiClientService.class);
        Appointment pending = Appointment.builder()
            .status(Appointment.AppointmentStatus.PENDING)
            .agendaType("CMSDF")
            .agendaBrief("Valid production appointment")
            .build();
        when(appointments.findAllProductionForDashboard()).thenReturn(List.of(pending));
        when(appointments.countProductionCreatedSince(any(LocalDateTime.class))).thenReturn(1L);
        when(aiClient.isAvailable()).thenReturn(false);

        Map<String, Object> result = new AiDocumentIntelligenceService(
            mock(OcrService.class), appointments, aiClient).getDashboardInsights();

        assertEquals(1, result.get("totalApplicationsThisMonth"));
        assertTrue(((String) result.get("aiNote")).contains("1 appointments"));
        assertFalse(Arrays.stream(Appointment.AppointmentStatus.values())
            .anyMatch(status -> "DUMMY".equals(status.name())));
        verify(appointments).findAllProductionForDashboard();
        verify(appointments).countProductionCreatedSince(any(LocalDateTime.class));
        verify(appointments, never()).findAll();
        verify(appointments, never()).countCreatedSince(any(LocalDateTime.class));
    }
}

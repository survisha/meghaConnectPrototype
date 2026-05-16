package com.survisha.meghaconnect.service;

import com.survisha.meghaconnect.dto.QrTokenGenerationResult;
import com.survisha.meghaconnect.entity.Appointment;
import com.survisha.meghaconnect.entity.AppointmentQrToken;
import com.survisha.meghaconnect.entity.Visitor;
import com.survisha.meghaconnect.repository.AppointmentQrTokenRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QrTokenServiceTest {

    @Mock
    private AppointmentQrTokenRepository appointmentQrTokenRepository;

    @InjectMocks
    private QrTokenService qrTokenService;

    @Test
    void generateRawTokenCreatesCryptographicallyStrongScannerSafeToken() {
        String first = qrTokenService.generateRawToken();
        String second = qrTokenService.generateRawToken();

        assertNotNull(first);
        assertEquals(64, first.length());
        assertTrue(first.matches("[A-F0-9]+"));
        assertNotEquals(first, second);
    }

    @Test
    void hashTokenUsesStableSha256HexHash() {
        String hash = qrTokenService.hashToken("secure-token");

        assertEquals(64, hash.length());
        assertEquals(hash, qrTokenService.hashToken(" secure-token "));
        assertNotEquals(hash, qrTokenService.hashToken("different-token"));
    }

    @Test
    void generateForApprovedAppointmentStoresOnlyHashAndReturnsRawTokenOnce() {
        Appointment appointment = Appointment.builder()
                .applicant(Visitor.builder().id(501L).fullName("Visitor").build())
                .scheduledDateTime(LocalDateTime.now().plusHours(1))
                .scheduledDurationMinutes(30)
                .build();
        appointment.setId(101L);

        when(appointmentQrTokenRepository.findTopByAppointment_IdAndStatusInOrderByGeneratedAtDesc(any(), any()))
                .thenReturn(Optional.empty());
        when(appointmentQrTokenRepository.findByTokenHash(any())).thenReturn(Optional.empty());
        when(appointmentQrTokenRepository.save(any(AppointmentQrToken.class))).thenAnswer(invocation -> invocation.getArgument(0));

        QrTokenGenerationResult result = qrTokenService.generateForApprovedAppointment(appointment, "admin");

        assertTrue(result.isNewlyGenerated());
        assertNotNull(result.getQrToken());
        assertEquals(AppointmentQrToken.QrStatus.ACTIVE, result.getStatus());
    }
}

package com.survisha.meghaconnect.service;

import com.survisha.meghaconnect.dto.QrScanRequest;
import com.survisha.meghaconnect.dto.QrValidationResponse;
import com.survisha.meghaconnect.entity.Appointment;
import com.survisha.meghaconnect.entity.AppointmentQrToken;
import com.survisha.meghaconnect.entity.QrScanAuditLog;
import com.survisha.meghaconnect.entity.Visitor;
import com.survisha.meghaconnect.exception.ErrorCodeConstants;
import com.survisha.meghaconnect.exception.MeghaConnectException;
import com.survisha.meghaconnect.repository.AppointmentQrTokenRepository;
import com.survisha.meghaconnect.repository.VisitorMovementLogRepository;
import com.survisha.meghaconnect.util.DateTimeUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class QrScannerServiceTest {

    @Mock
    private AppointmentQrTokenRepository appointmentQrTokenRepository;

    @Mock
    private VisitorMovementLogRepository visitorMovementLogRepository;

    @Mock
    private QrTokenService qrTokenService;

    @Mock
    private QrScanAuditService qrScanAuditService;

    @InjectMocks
    private QrScannerService qrScannerService;

    @BeforeEach
    void setUp() {
        lenient().when(qrTokenService.hashToken(any())).thenReturn("hashed-token");
    }

    @Test
    void validateAcceptsValidTokenHashAndReturnsVisitorDetails() {
        AppointmentQrToken token = activeToken();
        when(appointmentQrTokenRepository.findByTokenHashForUpdate(eq("hashed-token"), anyCollection(), anyCollection()))
                .thenReturn(Optional.of(token));

        QrValidationResponse response = qrScannerService.validate(
                request(),
                "security1",
                "SECURITY",
                "10.0.0.1",
                "scanner"
        );

        assertTrue(response.isValid());
        assertEquals(101L, response.getAppointmentId());
        assertEquals(501L, response.getVisitorId());
        assertEquals("ACTIVE", response.getQrStatus());
        assertEquals("NOT_CHECKED_IN", response.getMovementStatus());
        assertTrue(response.getCanCheckIn());
        verify(qrScanAuditService).record(
                eq("hashed-token"),
                eq(101L),
                eq(501L),
                eq("security1"),
                eq("device-1"),
                eq("Main Gate"),
                eq(QrScanAuditLog.ScanAction.VALIDATE),
                eq(QrScanAuditLog.ScanStatus.SUCCESS),
                isNull(),
                eq("10.0.0.1"),
                eq("scanner")
        );
    }

    @Test
    void validateAllowsDeoToLoadAuthorizedQrDetails() {
        AppointmentQrToken token = activeToken();
        when(appointmentQrTokenRepository.findByTokenHashForUpdate(eq("hashed-token"), anyCollection(), anyCollection()))
                .thenReturn(Optional.of(token));

        QrValidationResponse response = qrScannerService.validate(
                request(),
                "deo1",
                "ROLE_DEO",
                "10.0.0.1",
                "meghaconnect-mobile-deo1"
        );

        assertTrue(response.isValid());
        assertEquals(101L, response.getAppointmentId());
        assertEquals(501L, response.getVisitorId());
        verify(qrScanAuditService).record(
                eq("hashed-token"),
                eq(101L),
                eq(501L),
                eq("deo1"),
                eq("device-1"),
                eq("Main Gate"),
                eq(QrScanAuditLog.ScanAction.VALIDATE),
                eq(QrScanAuditLog.ScanStatus.SUCCESS),
                isNull(),
                eq("10.0.0.1"),
                eq("meghaconnect-mobile-deo1")
        );
    }

    @Test
    void validateRejectsExpiredQrAndMarksTokenExpired() {
        AppointmentQrToken token = activeToken();
        token.setValidTo(DateTimeUtil.nowIST().minusMinutes(1));
        when(appointmentQrTokenRepository.findByTokenHashForUpdate(eq("hashed-token"), anyCollection(), anyCollection()))
                .thenReturn(Optional.of(token));

        MeghaConnectException exception = assertThrows(
                MeghaConnectException.class,
                () -> qrScannerService.validate(request(), "security1", "SECURITY", "10.0.0.1", "scanner")
        );

        assertEquals(ErrorCodeConstants.QR_TOKEN_EXPIRED, exception.getErrorCode());
        assertEquals(AppointmentQrToken.QrStatus.EXPIRED, token.getStatus());
        verify(appointmentQrTokenRepository).save(token);
        verifyFailedAudit(QrScanAuditLog.ScanAction.VALIDATE);
    }

    @Test
    void checkInPreventsDuplicateCheckIn() {
        AppointmentQrToken token = activeToken();
        token.setStatus(AppointmentQrToken.QrStatus.CHECKED_IN);
        token.setCheckedInAt(DateTimeUtil.nowIST().minusMinutes(5));
        when(appointmentQrTokenRepository.findByTokenHashForUpdate(eq("hashed-token"), anyCollection(), anyCollection()))
                .thenReturn(Optional.of(token));

        MeghaConnectException exception = assertThrows(
                MeghaConnectException.class,
                () -> qrScannerService.checkIn(request(), "security1", "SECURITY", "10.0.0.1", "scanner")
        );

        assertEquals(ErrorCodeConstants.QR_DUPLICATE_CHECK_IN, exception.getErrorCode());
        verify(visitorMovementLogRepository, never()).save(any());
        verifyFailedAudit(QrScanAuditLog.ScanAction.CHECK_IN);
    }

    @Test
    void checkOutPreventsDuplicateCheckout() {
        AppointmentQrToken token = activeToken();
        token.setStatus(AppointmentQrToken.QrStatus.CHECKED_OUT);
        token.setCheckedInAt(DateTimeUtil.nowIST().minusMinutes(20));
        token.setCheckedOutAt(DateTimeUtil.nowIST().minusMinutes(5));
        when(appointmentQrTokenRepository.findByTokenHashForUpdate(eq("hashed-token"), anyCollection(), anyCollection()))
                .thenReturn(Optional.of(token));

        MeghaConnectException exception = assertThrows(
                MeghaConnectException.class,
                () -> qrScannerService.checkOut(request(), "security1", "SECURITY", "10.0.0.1", "scanner")
        );

        assertEquals(ErrorCodeConstants.QR_DUPLICATE_CHECK_OUT, exception.getErrorCode());
        verify(visitorMovementLogRepository, never()).save(any());
        verifyFailedAudit(QrScanAuditLog.ScanAction.CHECK_OUT);
    }

    @Test
    void validateRejectsUnauthorizedRole() {
        MeghaConnectException exception = assertThrows(
                MeghaConnectException.class,
                () -> qrScannerService.validate(request(), "visitor_1", "PUBLIC", "10.0.0.1", "scanner")
        );

        assertEquals(ErrorCodeConstants.INSUFFICIENT_PERMISSIONS, exception.getErrorCode());
        verifyNoInteractions(appointmentQrTokenRepository);
        verifyFailedAudit(QrScanAuditLog.ScanAction.VALIDATE);
    }

    @Test
    void validateRejectsInvalidToken() {
        when(appointmentQrTokenRepository.findByTokenHashForUpdate(eq("hashed-token"), anyCollection(), anyCollection()))
                .thenReturn(Optional.empty());

        MeghaConnectException exception = assertThrows(
                MeghaConnectException.class,
                () -> qrScannerService.validate(request(), "security1", "SECURITY", "10.0.0.1", "scanner")
        );

        assertEquals(ErrorCodeConstants.QR_TOKEN_INVALID, exception.getErrorCode());
        verifyFailedAudit(QrScanAuditLog.ScanAction.VALIDATE);
    }

    private void verifyFailedAudit(QrScanAuditLog.ScanAction action) {
        verify(qrScanAuditService).record(
                eq("hashed-token"),
                any(),
                any(),
                anyString(),
                eq("device-1"),
                eq("Main Gate"),
                eq(action),
                eq(QrScanAuditLog.ScanStatus.FAILED),
                any(),
                eq("10.0.0.1"),
                eq("scanner")
        );
    }

    private QrScanRequest request() {
        return QrScanRequest.builder()
                .qrToken("raw-token")
                .deviceId("device-1")
                .gateName("Main Gate")
                .build();
    }

    private AppointmentQrToken activeToken() {
        LocalDateTime now = DateTimeUtil.nowIST();
        Visitor visitor = Visitor.builder()
                .id(501L)
                .fullName("Visitor Name")
                .kycStatus("VERIFIED")
                .build();
        Appointment appointment = Appointment.builder()
                .applicant(visitor)
                .status(Appointment.AppointmentStatus.APPROVED_WITH_DATE_TIME)
                .scheduledDateTime(now)
                .scheduledDurationMinutes(30)
                .subject("Meeting")
                .department("CM Office")
                .approvedBy("approver")
                .build();
        appointment.setId(101L);

        return AppointmentQrToken.builder()
                .id(1L)
                .appointment(appointment)
                .visitor(visitor)
                .tokenHash("hashed-token")
                .status(AppointmentQrToken.QrStatus.ACTIVE)
                .validFrom(now.minusMinutes(30))
                .validTo(now.plusHours(2))
                .generatedBy("approver")
                .generatedAt(now.minusMinutes(30))
                .build();
    }
}

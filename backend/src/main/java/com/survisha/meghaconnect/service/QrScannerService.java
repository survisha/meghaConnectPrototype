package com.survisha.meghaconnect.service;

import com.survisha.meghaconnect.dto.QrActionResponse;
import com.survisha.meghaconnect.dto.QrScanRequest;
import com.survisha.meghaconnect.dto.QrValidationResponse;
import com.survisha.meghaconnect.entity.Appointment;
import com.survisha.meghaconnect.entity.AppointmentQrToken;
import com.survisha.meghaconnect.entity.QrScanAuditLog;
import com.survisha.meghaconnect.entity.Visitor;
import com.survisha.meghaconnect.entity.VisitorMovementLog;
import com.survisha.meghaconnect.exception.ErrorCodeConstants;
import com.survisha.meghaconnect.exception.MeghaConnectException;
import com.survisha.meghaconnect.repository.AppointmentQrTokenRepository;
import com.survisha.meghaconnect.repository.VisitorMovementLogRepository;
import com.survisha.meghaconnect.util.DateTimeUtil;
import com.survisha.meghaconnect.util.RequestContextUtil;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Slf4j
public class QrScannerService {

    private static final EnumSet<Appointment.AppointmentStatus> APPROVED_STATUSES = EnumSet.of(
            Appointment.AppointmentStatus.APPROVED_WITH_DATE_TIME,
            Appointment.AppointmentStatus.SCHEDULED_FOR_PUBLIC_DARBAR,
            Appointment.AppointmentStatus.SCHEDULED
    );

    private static final EnumSet<Appointment.AppointmentStatus> CANCELLED_STATUSES = EnumSet.of(
            Appointment.AppointmentStatus.CANCELLED,
            Appointment.AppointmentStatus.REJECTED,
            Appointment.AppointmentStatus.HCM_REJECTED
    );

    private final AppointmentQrTokenRepository appointmentQrTokenRepository;
    private final VisitorMovementLogRepository visitorMovementLogRepository;
    private final QrTokenService qrTokenService;
    private final QrScanAuditService qrScanAuditService;

    @Transactional(noRollbackFor = MeghaConnectException.class)
    public QrValidationResponse validate(QrScanRequest request,
                                         String actor,
                                         String actorRole,
                                         String ipAddress,
                                         String userAgent) {
        ScanContext context = contextFor(request, actor, QrScanAuditLog.ScanAction.VALIDATE);
        try {
            ensureScannerRole(actorRole);
            AppointmentQrToken qrToken = loadAndValidateForEntry(request, context, true);
            QrValidationResponse response = toValidationResponse(qrToken, "QR validated successfully.");
            recordSuccess(context, ipAddress, userAgent);
            return response;
        } catch (MeghaConnectException e) {
            recordFailure(context, e.getMessage(), ipAddress, userAgent);
            throw e;
        } catch (RuntimeException e) {
            recordFailure(context, "QR validation failed.", ipAddress, userAgent);
            throw e;
        }
    }

    @Transactional
    public QrActionResponse checkIn(QrScanRequest request,
                                    String actor,
                                    String actorRole,
                                    String ipAddress,
                                    String userAgent) {
        ScanContext context = contextFor(request, actor, QrScanAuditLog.ScanAction.CHECK_IN);
        try {
            ensureScannerRole(actorRole);
            AppointmentQrToken qrToken = loadAndValidateForEntry(request, context, false);
            if (qrToken.getCheckedInAt() != null
                    || qrToken.getStatus() == AppointmentQrToken.QrStatus.CHECKED_IN
                    || qrToken.getStatus() == AppointmentQrToken.QrStatus.CHECKED_OUT) {
                throw qrException(
                        ErrorCodeConstants.QR_DUPLICATE_CHECK_IN,
                        ErrorCodeConstants.QR_DUPLICATE_CHECK_IN_MSG,
                        HttpStatus.CONFLICT
                );
            }

            LocalDateTime now = DateTimeUtil.nowIST();
            qrToken.setStatus(AppointmentQrToken.QrStatus.CHECKED_IN);
            qrToken.setCheckedInAt(now);
            qrToken.setCheckedInBy(context.scannedBy);
            qrToken.setGateName(context.gateName);
            qrToken.setDeviceId(context.deviceId);
            appointmentQrTokenRepository.save(qrToken);
            saveMovement(qrToken, VisitorMovementLog.MovementType.ENTRY, context, now, "Visitor checked in");
            recordSuccess(context, ipAddress, userAgent);

            return QrActionResponse.builder()
                    .success(true)
                    .message("Visitor checked in successfully.")
                    .action("CHECK_IN")
                    .status("CHECKED_IN")
                    .movementStatus("CHECKED_IN")
                    .appointmentId(context.appointmentId)
                    .visitorId(context.visitorId)
                    .scanTime(now)
                    .build();
        } catch (MeghaConnectException e) {
            recordFailure(context, e.getMessage(), ipAddress, userAgent);
            throw e;
        } catch (RuntimeException e) {
            recordFailure(context, "QR check-in failed.", ipAddress, userAgent);
            throw e;
        }
    }

    @Transactional
    public QrActionResponse checkOut(QrScanRequest request,
                                     String actor,
                                     String actorRole,
                                     String ipAddress,
                                     String userAgent) {
        ScanContext context = contextFor(request, actor, QrScanAuditLog.ScanAction.CHECK_OUT);
        try {
            ensureScannerRole(actorRole);
            AppointmentQrToken qrToken = loadTokenForUpdate(request, context);
            if (qrToken.getCheckedOutAt() != null || qrToken.getStatus() == AppointmentQrToken.QrStatus.CHECKED_OUT) {
                throw qrException(
                        ErrorCodeConstants.QR_DUPLICATE_CHECK_OUT,
                        ErrorCodeConstants.QR_DUPLICATE_CHECK_OUT_MSG,
                        HttpStatus.CONFLICT
                );
            }
            if (qrToken.getCheckedInAt() == null || qrToken.getStatus() != AppointmentQrToken.QrStatus.CHECKED_IN) {
                throw qrException(
                        ErrorCodeConstants.QR_CHECK_OUT_REQUIRES_CHECK_IN,
                        ErrorCodeConstants.QR_CHECK_OUT_REQUIRES_CHECK_IN_MSG,
                        HttpStatus.CONFLICT
                );
            }

            LocalDateTime now = DateTimeUtil.nowIST();
            qrToken.setStatus(AppointmentQrToken.QrStatus.CHECKED_OUT);
            qrToken.setCheckedOutAt(now);
            qrToken.setCheckedOutBy(context.scannedBy);
            qrToken.setGateName(context.gateName);
            qrToken.setDeviceId(context.deviceId);
            appointmentQrTokenRepository.save(qrToken);
            saveMovement(qrToken, VisitorMovementLog.MovementType.EXIT, context, now, "Visitor checked out");
            recordSuccess(context, ipAddress, userAgent);

            return QrActionResponse.builder()
                    .success(true)
                    .message("Visitor checked out successfully.")
                    .action("CHECK_OUT")
                    .status("CHECKED_OUT")
                    .movementStatus("CHECKED_OUT")
                    .appointmentId(context.appointmentId)
                    .visitorId(context.visitorId)
                    .scanTime(now)
                    .build();
        } catch (MeghaConnectException e) {
            recordFailure(context, e.getMessage(), ipAddress, userAgent);
            throw e;
        } catch (RuntimeException e) {
            recordFailure(context, "QR check-out failed.", ipAddress, userAgent);
            throw e;
        }
    }

    private AppointmentQrToken loadAndValidateForEntry(QrScanRequest request,
                                                       ScanContext context,
                                                       boolean allowCheckedIn) {
        AppointmentQrToken qrToken = loadTokenForUpdate(request, context);
        LocalDateTime now = DateTimeUtil.nowIST();

        if (now.isBefore(qrToken.getValidFrom())) {
            throw qrException(
                    ErrorCodeConstants.QR_TOKEN_NOT_ACTIVE,
                    "QR token is not valid yet.",
                    HttpStatus.CONFLICT
            );
        }
        if (now.isAfter(qrToken.getValidTo())) {
            if (qrToken.getStatus() != AppointmentQrToken.QrStatus.CHECKED_OUT
                    && qrToken.getStatus() != AppointmentQrToken.QrStatus.REVOKED
                    && qrToken.getStatus() != AppointmentQrToken.QrStatus.CANCELLED) {
                qrToken.setStatus(AppointmentQrToken.QrStatus.EXPIRED);
                appointmentQrTokenRepository.save(qrToken);
            }
            throw qrException(
                    ErrorCodeConstants.QR_TOKEN_EXPIRED,
                    ErrorCodeConstants.QR_TOKEN_EXPIRED_MSG,
                    HttpStatus.GONE
            );
        }

        AppointmentQrToken.QrStatus status = qrToken.getStatus();
        if (status == AppointmentQrToken.QrStatus.QR_GENERATED) {
            qrToken.setStatus(AppointmentQrToken.QrStatus.ACTIVE);
            appointmentQrTokenRepository.save(qrToken);
        } else if (status == AppointmentQrToken.QrStatus.CHECKED_IN) {
            // A checked-in token can still be validated so guards can proceed to checkout.
            // For check-in actions, the caller returns a duplicate check-in error.
        } else if (status == AppointmentQrToken.QrStatus.CHECKED_OUT && !allowCheckedIn) {
            // Let check-in return the duplicate check-in business error consistently.
        } else if (status != AppointmentQrToken.QrStatus.ACTIVE) {
            throw qrException(
                    ErrorCodeConstants.QR_TOKEN_NOT_ACTIVE,
                    ErrorCodeConstants.QR_TOKEN_NOT_ACTIVE_MSG,
                    HttpStatus.CONFLICT
            );
        }

        validateAppointment(qrToken.getAppointment());
        validateVisitor(qrToken.getVisitor());
        return qrToken;
    }

    private AppointmentQrToken loadTokenForUpdate(QrScanRequest request, ScanContext context) {
        String qrToken = request != null ? trimToNull(firstNonBlank(request.getQrToken(), request.getQrData())) : null;
        if (qrToken == null) {
            throw qrException(
                    ErrorCodeConstants.QR_TOKEN_INVALID,
                    "QR token is required.",
                    HttpStatus.BAD_REQUEST
            );
        }
        return appointmentQrTokenRepository.findByTokenHashForUpdate(context.tokenHash)
                .map(token -> {
                    context.appointmentId = token.getAppointment() != null ? token.getAppointment().getId() : null;
                    context.visitorId = token.getVisitor() != null ? token.getVisitor().getId() : null;
                    return token;
                })
                .orElseThrow(() -> qrException(
                        ErrorCodeConstants.QR_TOKEN_INVALID,
                        ErrorCodeConstants.QR_TOKEN_INVALID_MSG,
                        HttpStatus.NOT_FOUND
                ));
    }

    private void validateAppointment(Appointment appointment) {
        if (appointment == null || appointment.getId() == null) {
            throw qrException(
                    ErrorCodeConstants.QR_APPOINTMENT_NOT_VALID,
                    "QR token is not linked to a valid appointment.",
                    HttpStatus.CONFLICT
            );
        }
        if (CANCELLED_STATUSES.contains(appointment.getStatus())) {
            throw qrException(
                    ErrorCodeConstants.QR_APPOINTMENT_NOT_VALID,
                    "Appointment is cancelled or rejected.",
                    HttpStatus.CONFLICT
            );
        }
        if (!APPROVED_STATUSES.contains(appointment.getStatus())) {
            throw qrException(
                    ErrorCodeConstants.QR_APPOINTMENT_NOT_VALID,
                    "Appointment is not approved.",
                    HttpStatus.CONFLICT
            );
        }
        if (appointment.getScheduledDateTime() == null) {
            throw qrException(
                    ErrorCodeConstants.QR_APPOINTMENT_NOT_VALID,
                    "Appointment is not scheduled.",
                    HttpStatus.CONFLICT
            );
        }
    }

    private void validateVisitor(Visitor visitor) {
        if (visitor == null || visitor.getId() == null) {
            throw qrException(
                    ErrorCodeConstants.QR_APPOINTMENT_NOT_VALID,
                    "Visitor is not linked to this QR token.",
                    HttpStatus.CONFLICT
            );
        }
        String kycStatus = visitor.getKycStatus();
        if ("BLOCKED".equalsIgnoreCase(kycStatus) || "CANCELLED".equalsIgnoreCase(kycStatus)) {
            throw qrException(
                    ErrorCodeConstants.QR_APPOINTMENT_NOT_VALID,
                    "Visitor is blocked or cancelled.",
                    HttpStatus.FORBIDDEN
            );
        }
    }

    private QrValidationResponse toValidationResponse(AppointmentQrToken qrToken, String message) {
        Appointment appointment = qrToken.getAppointment();
        Visitor visitor = qrToken.getVisitor();
        String movementStatus = movementStatus(qrToken);
        boolean canCheckIn = qrToken.getStatus() == AppointmentQrToken.QrStatus.ACTIVE
                && qrToken.getCheckedInAt() == null
                && qrToken.getCheckedOutAt() == null;
        boolean canCheckOut = qrToken.getStatus() == AppointmentQrToken.QrStatus.CHECKED_IN
                && qrToken.getCheckedInAt() != null
                && qrToken.getCheckedOutAt() == null;

        return QrValidationResponse.builder()
                .valid(true)
                .appointmentId(appointment != null ? appointment.getId() : null)
                .applicationId(appointment != null ? appointment.getApplicationId() : null)
                .applicantName(visitor != null ? visitor.getFullName() : null)
                .mobileNumber(visitor != null ? visitor.getPhoneNumber() : null)
                .visitorId(visitor != null ? visitor.getId() : null)
                .visitorName(visitor != null ? visitor.getFullName() : null)
                .visitorPhotoUrl(visitor != null ? firstNonBlank(
                        visitor.getLivePhotoPath(),
                        visitor.getPhotoStoragePath(),
                        visitor.getPhotoPath()
                ) : null)
                .appointmentDateTime(appointment != null ? appointment.getScheduledDateTime() : null)
                .scheduledDateTime(appointment != null ? appointment.getScheduledDateTime() : null)
                .location(appointment != null && appointment.getRequestedLocation() != null
                        ? appointment.getRequestedLocation().name()
                        : null)
                .status("VALID")
                .purpose(appointment != null ? firstNonBlank(
                        appointment.getSubject(),
                        appointment.getAgendaType(),
                        appointment.getAgendaBrief()
                ) : null)
                .department(appointment != null ? appointment.getDepartment() : null)
                .personToMeet(appointment != null ? firstNonBlank(
                        appointment.getApprovedBy(),
                        appointment.getCreatedBy(),
                        "CM Office"
                ) : null)
                .qrStatus(qrToken.getStatus().name())
                .movementStatus(movementStatus)
                .entryExitStatus(movementStatus)
                .canCheckIn(canCheckIn)
                .canCheckOut(canCheckOut)
                .message(message)
                .build();
    }

    private void saveMovement(AppointmentQrToken qrToken,
                              VisitorMovementLog.MovementType movementType,
                              ScanContext context,
                              LocalDateTime scanTime,
                              String remarks) {
        VisitorMovementLog movementLog = VisitorMovementLog.builder()
                .appointment(qrToken.getAppointment())
                .visitor(qrToken.getVisitor())
                .movementType(movementType)
                .scannedBy(context.scannedBy)
                .gateName(context.gateName)
                .deviceId(context.deviceId)
                .scanTime(scanTime)
                .remarks(remarks)
                .build();
        visitorMovementLogRepository.save(movementLog);
    }

    private ScanContext contextFor(QrScanRequest request, String actor, QrScanAuditLog.ScanAction action) {
        String rawToken = request != null ? firstNonBlank(request.getQrToken(), request.getQrData()) : null;
        return ScanContext.builder()
                .tokenHash(qrTokenService.hashToken(rawToken))
                .scannedBy(firstNonBlank(actor, "anonymous"))
                .deviceId(request != null ? limit(request.getDeviceId(), 150) : null)
                .gateName(request != null ? limit(firstNonBlank(request.getGateName(), request.getLocation()), 150) : null)
                .action(action)
                .build();
    }

    private void ensureScannerRole(String actorRole) {
        if (!hasRole(actorRole, "SECURITY", "ADMIN")) {
            throw qrException(
                    ErrorCodeConstants.INSUFFICIENT_PERMISSIONS,
                    ErrorCodeConstants.INSUFFICIENT_PERMISSIONS_MSG,
                    HttpStatus.FORBIDDEN
            );
        }
    }

    private boolean hasRole(String actorRole, String... allowedRoles) {
        if (actorRole == null) {
            return false;
        }
        String normalized = actorRole.replace("ROLE_", "").toUpperCase(Locale.ROOT);
        for (String allowedRole : allowedRoles) {
            if (allowedRole.equals(normalized)) {
                return true;
            }
        }
        return false;
    }

    private void recordSuccess(ScanContext context, String ipAddress, String userAgent) {
        qrScanAuditService.record(
                context.tokenHash,
                context.appointmentId,
                context.visitorId,
                context.scannedBy,
                context.deviceId,
                context.gateName,
                context.action,
                QrScanAuditLog.ScanStatus.SUCCESS,
                null,
                ipAddress,
                userAgent
        );
    }

    private void recordFailure(ScanContext context, String failureReason, String ipAddress, String userAgent) {
        qrScanAuditService.record(
                context.tokenHash,
                context.appointmentId,
                context.visitorId,
                context.scannedBy,
                context.deviceId,
                context.gateName,
                context.action,
                QrScanAuditLog.ScanStatus.FAILED,
                failureReason,
                ipAddress,
                userAgent
        );
    }

    private String movementStatus(AppointmentQrToken qrToken) {
        if (qrToken.getStatus() == AppointmentQrToken.QrStatus.CHECKED_OUT || qrToken.getCheckedOutAt() != null) {
            return "CHECKED_OUT";
        }
        if (qrToken.getStatus() == AppointmentQrToken.QrStatus.CHECKED_IN || qrToken.getCheckedInAt() != null) {
            return "CHECKED_IN";
        }
        return "NOT_CHECKED_IN";
    }

    private MeghaConnectException qrException(String code, String message, HttpStatus status) {
        return new MeghaConnectException(code, message, status.value());
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            String trimmed = trimToNull(value);
            if (trimmed != null) {
                return trimmed;
            }
        }
        return null;
    }

    private String trimToNull(String value) {
        return value == null || value.trim().isEmpty() ? null : value.trim();
    }

    private String limit(String value, int maxLength) {
        String trimmed = trimToNull(value);
        if (trimmed == null) {
            return null;
        }
        return trimmed.length() <= maxLength ? trimmed : trimmed.substring(0, maxLength);
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    private static class ScanContext {
        private String tokenHash;
        private Long appointmentId;
        private Long visitorId;
        private String scannedBy;
        private String deviceId;
        private String gateName;
        private QrScanAuditLog.ScanAction action;
    }
}

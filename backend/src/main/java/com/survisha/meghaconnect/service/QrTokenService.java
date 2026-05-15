package com.survisha.meghaconnect.service;

import com.survisha.meghaconnect.dto.QrTokenGenerationResult;
import com.survisha.meghaconnect.entity.Appointment;
import com.survisha.meghaconnect.entity.AppointmentQrToken;
import com.survisha.meghaconnect.entity.Visitor;
import com.survisha.meghaconnect.exception.ErrorCodeConstants;
import com.survisha.meghaconnect.exception.MeghaConnectException;
import com.survisha.meghaconnect.repository.AppointmentQrTokenRepository;
import com.survisha.meghaconnect.util.DateTimeUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;

@Service
@RequiredArgsConstructor
public class QrTokenService {

    private static final int TOKEN_BYTES = 32;
    private static final List<AppointmentQrToken.QrStatus> REUSABLE_STATUSES = List.of(
            AppointmentQrToken.QrStatus.QR_GENERATED,
            AppointmentQrToken.QrStatus.ACTIVE,
            AppointmentQrToken.QrStatus.CHECKED_IN
    );

    private final AppointmentQrTokenRepository appointmentQrTokenRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${meghaconnect.qr.valid-before-minutes:240}")
    private long validBeforeMinutes;

    @Value("${meghaconnect.qr.valid-after-minutes:720}")
    private long validAfterMinutes;

    @Transactional
    public QrTokenGenerationResult generateForApprovedAppointment(Appointment appointment, String actor) {
        if (appointment == null || appointment.getId() == null) {
            throw new MeghaConnectException(
                    ErrorCodeConstants.QR_GENERATION_FAILED,
                    "Appointment must be saved before QR token generation.",
                    HttpStatus.BAD_REQUEST.value()
            );
        }
        Visitor visitor = appointment.getApplicant();
        if (visitor == null || visitor.getId() == null) {
            throw new MeghaConnectException(
                    ErrorCodeConstants.QR_GENERATION_FAILED,
                    "Appointment visitor is required for QR token generation.",
                    HttpStatus.BAD_REQUEST.value()
            );
        }

        var existingToken = appointmentQrTokenRepository
                .findTopByAppointment_IdAndStatusInOrderByGeneratedAtDesc(appointment.getId(), REUSABLE_STATUSES);
        if (existingToken.isPresent()) {
            AppointmentQrToken existing = existingToken.get();
            return QrTokenGenerationResult.builder()
                    .appointmentId(appointment.getId())
                    .visitorId(visitor.getId())
                    .status(existing.getStatus())
                    .validFrom(existing.getValidFrom())
                    .validTo(existing.getValidTo())
                    .newlyGenerated(false)
                    .build();
        }

        String rawToken;
        String tokenHash;
        do {
            rawToken = generateRawToken();
            tokenHash = hashToken(rawToken);
        } while (appointmentQrTokenRepository.findByTokenHash(tokenHash).isPresent());

        LocalDateTime generatedAt = DateTimeUtil.nowIST();
        LocalDateTime scheduledAt = appointment.getScheduledDateTime() != null
                ? appointment.getScheduledDateTime()
                : generatedAt;
        int durationMinutes = appointment.getScheduledDurationMinutes() != null
                ? appointment.getScheduledDurationMinutes()
                : 30;
        LocalDateTime validFrom = scheduledAt.minusMinutes(validBeforeMinutes);
        LocalDateTime validTo = scheduledAt.plusMinutes(durationMinutes).plusMinutes(validAfterMinutes);

        AppointmentQrToken qrToken = AppointmentQrToken.builder()
                .appointment(appointment)
                .visitor(visitor)
                .tokenHash(tokenHash)
                .status(AppointmentQrToken.QrStatus.ACTIVE)
                .validFrom(validFrom)
                .validTo(validTo)
                .generatedBy(firstNonBlank(actor, "system"))
                .generatedAt(generatedAt)
                .build();
        AppointmentQrToken saved = appointmentQrTokenRepository.save(qrToken);

        return QrTokenGenerationResult.builder()
                .appointmentId(appointment.getId())
                .visitorId(visitor.getId())
                .qrToken(rawToken)
                .status(saved.getStatus())
                .validFrom(saved.getValidFrom())
                .validTo(saved.getValidTo())
                .newlyGenerated(true)
                .build();
    }

    public String generateRawToken() {
        byte[] bytes = new byte[TOKEN_BYTES];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public String hashToken(String rawToken) {
        String normalized = rawToken == null ? "" : rawToken.trim();
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(normalized.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder(hash.length * 2);
            for (byte value : hash) {
                builder.append(String.format("%02x", value));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available.", e);
        }
    }

    private String firstNonBlank(String value, String fallback) {
        return value == null || value.trim().isEmpty() ? fallback : value.trim();
    }

}

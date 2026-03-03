package com.survisha.meghaconnect.service;

import com.survisha.meghaconnect.entity.OtpTemp;
import com.survisha.meghaconnect.entity.Person;
import com.survisha.meghaconnect.repository.OtpTempRepository;
import com.survisha.meghaconnect.repository.PersonRepository;
import com.survisha.meghaconnect.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Handles OTP generation and validation for visitor (citizen) mobile login.
 *
 * Security controls:
 *  - OTPs expire after OTP_VALIDITY_MINUTES.
 *  - Each OTP may be attempted at most MAX_OTP_ATTEMPTS times before it is locked.
 *  - If more than MAX_OTP_REQUESTS OTP requests are made from the same phone number
 *    within RATE_WINDOW_MINUTES, further requests are rejected (rate limiting).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class VisitorOtpService {

    private static final int OTP_VALIDITY_MINUTES = 5;
    private static final int MAX_OTP_ATTEMPTS     = 5;
    private static final int MAX_OTP_REQUESTS     = 10;
    private static final int RATE_WINDOW_MINUTES  = 60;

    /** Shared SecureRandom instance – thread-safe and expensive to initialize. */
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final PersonRepository    personRepository;
    private final OtpTempRepository   otpTempRepository;
    private final JwtService          jwtService;

    // ── Check mobile ─────────────────────────────────────────────────────────

    public boolean isMobileRegistered(String phone) {
        return personRepository.findByPhoneNumber(phone).isPresent();
    }

    // ── Generate OTP ─────────────────────────────────────────────────────────

    /**
     * Generates a 6-digit OTP for the given phone number, persists it, and
     * returns it (later this value is sent via SMS gateway).
     *
     * @throws IllegalArgumentException  if mobile not registered
     * @throws IllegalStateException     if rate limit exceeded
     */
    @Transactional
    public String generateOtp(String phone) {
        if (!isMobileRegistered(phone)) {
            throw new IllegalArgumentException("MOBILE_NOT_FOUND");
        }

        // Rate-limit check
        LocalDateTime windowStart = LocalDateTime.now().minusMinutes(RATE_WINDOW_MINUTES);
        int recentRequests = otpTempRepository.sumAttemptCountSince(phone, windowStart);
        if (recentRequests >= MAX_OTP_REQUESTS) {
            throw new IllegalStateException("OTP_RATE_LIMIT_EXCEEDED");
        }

        String otpCode = generateSixDigitOtp();
        OtpTemp record = OtpTemp.builder()
                .phoneNumber(phone)
                .otpCode(otpCode)
                .expiresAt(LocalDateTime.now().plusMinutes(OTP_VALIDITY_MINUTES))
                .consumed(false)
                .attemptCount(0)
                .build();
        otpTempRepository.save(record);

        // TODO: integrate SMS gateway (MSG91 / CDAC) to send `otpCode` to `phone`
        log.info("OTP generated for {} (mock – SMS gateway pending): {}", phone, otpCode);
        return otpCode;
    }

    // ── Validate OTP & issue JWT ──────────────────────────────────────────────

    /**
     * Validates the submitted OTP and, if correct, returns a JWT token for the visitor.
     *
     * @throws IllegalArgumentException  if mobile not found or OTP is wrong
     * @throws IllegalStateException     if OTP expired, consumed, or max attempts exceeded
     */
    @Transactional
    public String validateOtpAndLogin(String phone, String submittedOtp) {
        Optional<OtpTemp> optRecord =
                otpTempRepository.findTopByPhoneNumberAndConsumedFalseAndExpiresAtAfterOrderByCreatedAtDesc(
                        phone, LocalDateTime.now());

        if (optRecord.isEmpty()) {
            throw new IllegalStateException("OTP_EXPIRED_OR_NOT_FOUND");
        }

        OtpTemp record = optRecord.get();

        if (record.getAttemptCount() >= MAX_OTP_ATTEMPTS) {
            throw new IllegalStateException("OTP_MAX_ATTEMPTS_EXCEEDED");
        }

        if (!record.getOtpCode().equals(submittedOtp)) {
            record.setAttemptCount(record.getAttemptCount() + 1);
            otpTempRepository.save(record);
            int remaining = MAX_OTP_ATTEMPTS - record.getAttemptCount();
            throw new IllegalArgumentException("OTP_INVALID:" + remaining);
        }

        // Mark OTP consumed to prevent replay
        record.setConsumed(true);
        otpTempRepository.save(record);

        // Build a minimal UserDetails to feed into JwtService
        Person visitor = personRepository.findByPhoneNumber(phone)
                .orElseThrow(() -> new IllegalArgumentException("MOBILE_NOT_FOUND"));

        UserDetails userDetails = User.builder()
                .username("visitor_" + visitor.getId())
                .password("")
                .authorities(List.of(new SimpleGrantedAuthority("ROLE_PUBLIC")))
                .build();

        return jwtService.generateToken(userDetails);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private String generateSixDigitOtp() {
        int code = 100000 + SECURE_RANDOM.nextInt(900000);
        return String.valueOf(code);
    }
}

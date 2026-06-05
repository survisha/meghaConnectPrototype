package com.survisha.meghaconnect.service;

import com.survisha.common.sms.SmsService;
import com.survisha.meghaconnect.entity.OtpTemp;
import com.survisha.meghaconnect.entity.Visitor;
import com.survisha.meghaconnect.repository.OtpTempRepository;
import com.survisha.meghaconnect.repository.VisitorRepository;
import com.survisha.meghaconnect.security.JwtService;
import com.survisha.meghaconnect.exception.*;
import com.survisha.meghaconnect.util.DateTimeUtil;
import com.survisha.meghaconnect.util.RequestContextUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Collections;
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

    private final VisitorRepository  visitorRepository;
    private final OtpTempRepository   otpTempRepository;
    private final JwtService          jwtService;
    private final SmsService          smsService;

    // ── Check mobile ─────────────────────────────────────────────────────────

    public boolean isMobileRegistered(String phone) {
        return !visitorRepository.findByPhoneNumber(phone).isEmpty();
    }

    // ── Generate OTP ─────────────────────────────────────────────────────────

    /**
     * Generates a 6-digit OTP for the given phone number, persists it, and
     * returns it (later this value is sent via SMS gateway).
     *
     * @throws VisitorRegistrationValidationException  if mobile not registered
     * @throws OtpRateLimitExceededException     if rate limit exceeded
     */
    @Transactional
    public String generateOtp(String phone) {
        List<Visitor> visitors = visitorRepository.findByPhoneNumber(phone);
        if (visitors.isEmpty()) {
            throw new VisitorRegistrationValidationException(
                ErrorCodeConstants.MOBILE_NOT_FOUND,
                ErrorCodeConstants.format(ErrorCodeConstants.MOBILE_NOT_FOUND_MSG, phone)
            );
        }

        Long visitorId = visitors.size() == 1 ? visitors.get(0).getId() : null;
        return generateOtpRecord(phone, visitorId, "LOGIN");
    }

    @Transactional
    public String generateOtp(String phone, Long visitorId) {
        if (visitorId == null || !visitorRepository.findById(visitorId).isPresent()) {
            throw new VisitorRegistrationValidationException(
                    ErrorCodeConstants.VISITOR_NOT_FOUND,
                    "Visitor not found"
            );
        }

        return generateOtpRecord(phone, visitorId, "LOGIN");
    }

    private String generateOtpRecord(String phone, Long visitorId, String purpose) {
        // Rate-limit check
        LocalDateTime windowStart = DateTimeUtil.nowIST().minusMinutes(RATE_WINDOW_MINUTES);
        int recentRequests = otpTempRepository.sumAttemptCountSince(phone, windowStart);
        if (recentRequests >= MAX_OTP_REQUESTS) {
            int waitTimeMinutes = RATE_WINDOW_MINUTES;
            throw new OtpRateLimitExceededException(waitTimeMinutes);
        }

        String otpCode = generateSixDigitOtp();
        OtpTemp record = OtpTemp.builder()
                .phoneNumber(phone)
                .visitorId(visitorId)
                .otpCode(otpCode)
                .expiresAt(DateTimeUtil.nowIST().plusMinutes(OTP_VALIDITY_MINUTES))
                .consumed(false)
                .attemptCount(0)
                .build();
        otpTempRepository.save(record);

        sendOtpSms(phone, otpCode, purpose);
        log.info("OTP generated purpose={} phone={} visitorId={}",
                purpose, RequestContextUtil.maskPhone(phone), visitorId);
        return otpCode;
    }

    // ── Validate OTP & issue JWT ──────────────────────────────────────────────

    /**
     * Validates the submitted OTP and, if correct, returns a JWT token for the visitor.
     *
     * @throws OtpExpiredException  if OTP expired or not found
     * @throws OtpMaxAttemptsExceededException if max attempts exceeded
     * @throws OtpValidationFailedException if OTP is wrong
     */
    @Transactional(noRollbackFor = {OtpValidationFailedException.class, OtpMaxAttemptsExceededException.class})
    public String validateOtpAndLogin(String phone, String submittedOtp) {
        List<Visitor> visitors = visitorRepository.findByPhoneNumber(phone);
        if (visitors.size() != 1) {
            throw new VisitorRegistrationValidationException(
                    ErrorCodeConstants.INVALID_VISITOR_DATA,
                    "Unable to resolve a single visitor for this mobile number"
            );
        }

        return validateOtpAndLogin(phone, submittedOtp, visitors.get(0).getId());
    }

    @Transactional(noRollbackFor = {OtpValidationFailedException.class, OtpMaxAttemptsExceededException.class})
    public String validateOtpAndLogin(String phone, String submittedOtp, Long visitorId) {
        Optional<OtpTemp> optRecord =
                otpTempRepository.findTopByPhoneNumberAndVisitorIdAndConsumedFalseOrderByCreatedAtDesc(
                        phone, visitorId);

        if (!optRecord.isPresent()) {
            throw new OtpExpiredException();
        }

        OtpTemp record = optRecord.get();

        if (record.getAttemptCount() >= MAX_OTP_ATTEMPTS) {
            throw new OtpMaxAttemptsExceededException();
        }

        if (record.getExpiresAt() == null || record.getExpiresAt().isBefore(DateTimeUtil.nowIST())) {
            throw new OtpExpiredException();
        }

        if (!record.getOtpCode().equals(submittedOtp)) {
            record.setAttemptCount(record.getAttemptCount() + 1);
            otpTempRepository.save(record);
            int remaining = MAX_OTP_ATTEMPTS - record.getAttemptCount();
            if (remaining <= 0) {
                throw new OtpMaxAttemptsExceededException();
            }
            throw new OtpValidationFailedException(remaining);
        }

        // Mark OTP consumed to prevent replay
        record.setConsumed(true);
        otpTempRepository.save(record);

        // Build a minimal UserDetails to feed into JwtService
        Visitor visitor = visitorRepository.findById(visitorId)
                .orElseThrow(() -> new VisitorRegistrationValidationException(
                    ErrorCodeConstants.VISITOR_NOT_FOUND,
                    "Visitor not found"
                ));

        UserDetails userDetails = User.builder()
                .username("visitor_" + visitor.getId())
                .password("")
                .authorities(Collections.singletonList(new SimpleGrantedAuthority("ROLE_PUBLIC")))
                .build();

        return jwtService.generateToken(userDetails);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private String generateSixDigitOtp() {
        int code = 100000 + SECURE_RANDOM.nextInt(900000);
        return String.valueOf(code);
    }

    // ── KYC-specific OTP methods (MOCK) ──────────────────────────────────────

    /**
     * Generates an OTP for KYC validation flow.
     * 
     * Unlike generateOtp(), this does NOT require the phone number to be registered yet,
     * as the visitor is still in the registration KYC flow.
     *
     * @throws OtpRateLimitExceededException if rate limit exceeded
     */
    @Transactional
    public String generateKycOtp(String phone) {
        // Rate-limit check (same logic as regular OTP)
        LocalDateTime windowStart = DateTimeUtil.nowIST().minusMinutes(RATE_WINDOW_MINUTES);
        int recentRequests = otpTempRepository.sumAttemptCountSince(phone, windowStart);
        if (recentRequests >= MAX_OTP_REQUESTS) {
            int waitTimeMinutes = RATE_WINDOW_MINUTES;
            throw new OtpRateLimitExceededException(waitTimeMinutes);
        }

        String otpCode = generateSixDigitOtp();
        
        OtpTemp record = OtpTemp.builder()
                .phoneNumber(phone)
                .visitorId(null)
                .otpCode(otpCode)
                .expiresAt(DateTimeUtil.nowIST().plusMinutes(OTP_VALIDITY_MINUTES))
                .consumed(false)
                .attemptCount(0)
                .build();
        otpTempRepository.save(record);

        smsService.sendOtpSms(phone, otpCode);
        log.info("KYC OTP generated for phone={}", RequestContextUtil.maskPhone(phone));
        return otpCode;
    }

    private void sendOtpSms(String phone, String otpCode, String purpose) {
        if ("REGISTRATION".equalsIgnoreCase(purpose)) {
            smsService.sendOtpSms(phone, otpCode);
            return;
        }
        smsService.sendLoginOtpSms(phone, otpCode);
    }

    /**
     * Validates the OTP for KYC flow (does not issue JWT).
     * 
     * @return true if OTP is valid, false otherwise
     * @throws OtpExpiredException if OTP expired or not found
     * @throws OtpMaxAttemptsExceededException if max attempts exceeded
     */
    @Transactional
    public boolean validateKycOtp(String phone, String submittedOtp) {
        Optional<OtpTemp> optRecord =
                otpTempRepository.findTopByPhoneNumberAndVisitorIdIsNullAndConsumedFalseOrderByCreatedAtDesc(phone);

        if (!optRecord.isPresent()) {
            throw new OtpExpiredException();
        }

        OtpTemp record = optRecord.get();

        if (record.getAttemptCount() >= MAX_OTP_ATTEMPTS) {
            throw new OtpMaxAttemptsExceededException();
        }

        if (record.getExpiresAt() == null || record.getExpiresAt().isBefore(DateTimeUtil.nowIST())) {
            throw new OtpExpiredException();
        }

        if (!record.getOtpCode().equals(submittedOtp)) {
            record.setAttemptCount(record.getAttemptCount() + 1);
            otpTempRepository.save(record);
            return false;
        }

        // Mark OTP consumed to prevent replay
        record.setConsumed(true);
        otpTempRepository.save(record);

        return true;
    }
}

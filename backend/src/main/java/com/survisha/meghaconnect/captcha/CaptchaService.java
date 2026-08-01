package com.survisha.meghaconnect.captcha;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;


import com.survisha.meghaconnect.captcha.CaptchaDtos.CaptchaResponse;
import com.survisha.meghaconnect.captcha.CaptchaDtos.CaptchaValidateResponse;
import com.survisha.meghaconnect.exception.MeghaConnectException;
import com.survisha.meghaconnect.util.DateTimeUtil;

@Service
public class CaptchaService {
    private static final Logger log = LoggerFactory.getLogger(CaptchaService.class);
    private final CaptchaGenerator generator;
    private final CaptchaStoreService store;
    private final long expiryMinutes;
    private final boolean enabled;

    public CaptchaService(CaptchaGenerator generator, CaptchaStoreService store,
                          @Value("${captcha.expiry-minutes:5}") long expiryMinutes,
                          @Value("${captcha.enabled:true}") boolean enabled) {
        this.generator = generator;
        this.store = store;
        this.expiryMinutes = expiryMinutes;
        this.enabled = enabled;
    }

    public CaptchaResponse generate() {
        String captchaId = UUID.randomUUID().toString();
        String captchaValue = generator.generateText();
        Instant expiresAt = DateTimeUtil.nowISTInstant().plus(expiryMinutes, ChronoUnit.MINUTES);
        String image = generator.generateImage(captchaValue);
        store.save(captchaId, captchaValue, expiresAt);
        log.info("Captcha generated captchaId={}", captchaId);
        return new CaptchaResponse(captchaId, image, null, expiresAt.toString());
    }

    public CaptchaValidateResponse validate(String captchaId, String captchaValue) {
        validateOrThrow(captchaId, captchaValue);
        return new CaptchaValidateResponse(true, "Captcha validated successfully");
    }

    public void validateForLogin(String captchaId, String captchaValue) {
        if (!enabled) {
            return;
        }
        try {
            validateOrThrow(captchaId, captchaValue);
        } catch (MeghaConnectException ex) {
            if ("CAPTCHA_EXPIRED".equals(ex.getErrorCode())) {
                throw ex;
            }
            throw invalidCaptcha();
        }
    }

    private void validateOrThrow(String captchaId, String captchaValue) {
        var entry = store.remove(captchaId).orElse(null);
        if (entry == null) {
            log.warn("Captcha validation failed captchaId={}", captchaId);
            throw invalidCaptcha();
        }
        if (entry.expiresAt().isBefore(DateTimeUtil.nowISTInstant())) {
            log.warn("Captcha validation expired captchaId={}", captchaId);
            throw new MeghaConnectException("CAPTCHA_EXPIRED", "Captcha expired", 400);
        }
        if (captchaValue == null || !entry.value().equals(captchaValue.trim().toUpperCase(Locale.ROOT))) {
            log.warn("Captcha validation failed captchaId={}", captchaId);
            throw invalidCaptcha();
        }
        log.info("Captcha validation succeeded captchaId={}", captchaId);
    }

    private MeghaConnectException invalidCaptcha() {
        return new MeghaConnectException("INVALID_CAPTCHA", "Invalid captcha", 400);
    }
}

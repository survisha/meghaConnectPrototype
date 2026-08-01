package com.survisha.meghaconnect.captcha;

import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

import com.survisha.meghaconnect.util.DateTimeUtil;

@Service
public class CaptchaStoreService {
    private final Map<String, CaptchaEntry> store = new ConcurrentHashMap<>();

    public void save(String captchaId, String captchaValue, Instant expiresAt) {
        cleanupExpired();
        store.put(captchaId, new CaptchaEntry(captchaValue.toUpperCase(Locale.ROOT), expiresAt));
    }

    public Optional<CaptchaEntry> remove(String captchaId) {
        return Optional.ofNullable(store.remove(captchaId));
    }

    private void cleanupExpired() {
        Instant now = DateTimeUtil.nowISTInstant();
        store.entrySet().removeIf(entry -> entry.getValue().expiresAt().isBefore(now));
    }

    public record CaptchaEntry(String value, Instant expiresAt) { }
}

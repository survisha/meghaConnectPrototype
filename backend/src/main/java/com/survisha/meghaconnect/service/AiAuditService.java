package com.survisha.meghaconnect.service;

import com.survisha.meghaconnect.entity.AiCallAudit;
import com.survisha.meghaconnect.repository.AiCallAuditRepository;
import com.survisha.meghaconnect.util.RequestContextUtil;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiAuditService {

    private final AiCallAuditRepository repository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(String moduleName,
                       String promptType,
                       String provider,
                       String model,
                       LocalDateTime requestTime,
                       long durationMs,
                       boolean success,
                       String errorMessage) {
        try {
            repository.save(AiCallAudit.builder()
                    .requestId(RequestContextUtil.getRequestId())
                    .moduleName(limit(moduleName, 80))
                    .userId(limit(currentUser(), 100))
                    .promptType(limit(promptType, 80))
                    .provider(limit(provider, 40))
                    .model(limit(model, 120))
                    .requestTime(requestTime)
                    .durationMs(durationMs)
                    .success(success)
                    .errorMessage(limit(errorMessage, 500))
                    .build());
        } catch (Exception e) {
            log.warn("Failed to write AI audit requestId={} error={}",
                    RequestContextUtil.getRequestId(), e.getClass().getSimpleName());
        }
    }

    private String currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        String name = authentication.getName();
        return name == null || "anonymousUser".equals(name) ? null : name;
    }

    private String limit(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.length() > maxLength ? normalized.substring(0, maxLength) : normalized;
    }
}

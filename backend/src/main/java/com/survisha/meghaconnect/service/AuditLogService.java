package com.survisha.meghaconnect.service;

import com.survisha.meghaconnect.entity.AuditLog;
import com.survisha.meghaconnect.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    @Transactional
    public void log(String entityType, Long entityId, String action,
                    String details, String performedBy) {
        AuditLog log = AuditLog.builder()
            .entityType(entityType)
            .entityId(entityId)
            .action(action)
            .details(details)
            .performedBy(performedBy)
            .timestamp(LocalDateTime.now())
            .build();
        auditLogRepository.save(log);
    }
}

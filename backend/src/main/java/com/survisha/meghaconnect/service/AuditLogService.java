package com.survisha.meghaconnect.service;

import com.survisha.meghaconnect.entity.AuditLog;
import com.survisha.meghaconnect.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    @Transactional
    public void log(String entityType, Long entityId, String action,
                    String details, String performedBy) {
        log.debug("Logging audit action - Entity: {}, Action: {}, PerformedBy: {}", 
            entityType, action, performedBy);
            
        AuditLog auditLog = AuditLog.builder()
            .entityType(entityType)
            .entityId(entityId)
            .action(action)
            .details(details)
            .performedBy(performedBy)
            .timestamp(LocalDateTime.now())
            .build();
        auditLogRepository.save(auditLog);
    }

    @Transactional(readOnly = true)
    public Page<AuditLog> getAllAuditLogs(Pageable pageable) {
        log.debug("Fetching audit logs with pagination");
        return auditLogRepository.findAll(pageable);
    }
}

package com.survisha.meghaconnect.service;

import com.survisha.meghaconnect.dto.AuditLogDto;
import com.survisha.meghaconnect.dto.AuditLogFilterRequest;
import com.survisha.meghaconnect.entity.AuditLog;
import com.survisha.meghaconnect.entity.User;
import com.survisha.meghaconnect.repository.AuditLogRepository;
import com.survisha.meghaconnect.repository.UserRepository;
import com.survisha.meghaconnect.util.DateTimeUtil;
import com.survisha.meghaconnect.util.RequestContextUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;

    @Transactional
    public void log(String entityType, Long entityId, String action,
                    String details, String performedBy) {
        log.debug("Logging audit action - Entity: {}, Action: {}, PerformedBy: {}", 
            entityType, action, performedBy);
            
        Optional<User> actor = hasText(performedBy)
                ? userRepository.findByNormalizedUsername(performedBy)
                : Optional.empty();
        AuditLog auditLog = AuditLog.builder()
            .department(actor.map(User::getDepartment).orElse(null))
            .entityType(entityType)
            .entityId(entityId)
            .action(action)
            .details(details)
            .performedBy(performedBy)
            .role(actor.map(User::getRole).map(Enum::name).orElseGet(() -> resolveRole(performedBy)))
            .requestId(RequestContextUtil.getRequestId())
            .status("SUCCESS")
            .timestamp(DateTimeUtil.nowIST())
            .build();
        auditLogRepository.save(auditLog);
    }

    @Transactional(readOnly = true)
    public Page<AuditLogDto> getAllAuditLogs(Pageable pageable, AuditLogFilterRequest filter) {
        log.debug("Fetching audit logs with pagination");
        User actor = currentActor();
        Long enforcedDepartmentId = null;
        if (actor.getRole() == User.UserRole.DEPARTMENT_ADMIN) {
            if (actor.getDepartment() == null || actor.getDepartment().getId() == null) {
                throw new org.springframework.security.access.AccessDeniedException(
                        "Department Admin is not assigned to a department");
            }
            enforcedDepartmentId = actor.getDepartment().getId();
        } else if (actor.getRole() != User.UserRole.SUPER_ADMIN && actor.getRole() != User.UserRole.ADMIN) {
            throw new org.springframework.security.access.AccessDeniedException("Access denied");
        }
        return auditLogRepository.findAll(buildSpecification(filter, enforcedDepartmentId), pageable)
                .map(this::toDto);
    }

    private Specification<AuditLog> buildSpecification(AuditLogFilterRequest filter, Long enforcedDepartmentId) {
        return (root, query, cb) -> {
            var predicate = cb.conjunction();
            if (enforcedDepartmentId != null) {
                predicate = cb.and(predicate,
                        cb.equal(root.get("department").get("id"), enforcedDepartmentId));
            }
            if (filter == null) return predicate;
            if (hasText(filter.getModule())) {
                predicate = cb.and(predicate, cb.like(cb.lower(root.get("entityType")), like(filter.getModule())));
            }
            if (hasText(filter.getAction())) {
                predicate = cb.and(predicate, cb.like(cb.lower(root.get("action")), like(filter.getAction())));
            }
            if (hasText(filter.getUser())) {
                predicate = cb.and(predicate, cb.like(cb.lower(root.get("performedBy")), like(filter.getUser())));
            }
            if (hasText(filter.getRole())) {
                predicate = cb.and(predicate, cb.equal(cb.upper(root.get("role")), filter.getRole().trim().toUpperCase(Locale.ROOT)));
            }
            if (hasText(filter.getRequestId())) {
                predicate = cb.and(predicate, cb.like(cb.lower(root.get("requestId")), like(filter.getRequestId())));
            }
            if (hasText(filter.getStatus())) {
                predicate = cb.and(predicate, cb.equal(cb.upper(root.get("status")), filter.getStatus().trim().toUpperCase(Locale.ROOT)));
            }

            LocalDateTime from = parseDateTime(filter.getFrom());
            LocalDateTime to = parseDateTime(filter.getTo());
            if (from != null) {
                predicate = cb.and(predicate, cb.greaterThanOrEqualTo(root.get("timestamp"), from));
            }
            if (to != null) {
                predicate = cb.and(predicate, cb.lessThanOrEqualTo(root.get("timestamp"), to));
            }
            return predicate;
        };
    }

    private AuditLogDto toDto(AuditLog log) {
        String entityType = log.getEntityType();
        String details = log.getDetails();
        String performedBy = log.getPerformedBy();
        String role = log.getRole();
        return AuditLogDto.builder()
                .id(log.getId())
                .timestamp(log.getTimestamp())
                .module(entityType)
                .entity(entityType)
                .entityType(entityType)
                .entityId(log.getEntityId())
                .action(log.getAction())
                .user(performedBy)
                .performedBy(performedBy)
                .role(role)
                .userRole(role)
                .requestId(log.getRequestId())
                .oldValue(log.getOldValue())
                .newValue(log.getNewValue())
                .description(details)
                .details(details)
                .status(log.getStatus())
                .ipAddress(log.getIpAddress())
                .endpoint(log.getEndpoint())
                .build();
    }

    private String resolveRole(String performedBy) {
        if (!hasText(performedBy)) {
            return "";
        }
        if (performedBy.startsWith("visitor_")) {
            return "PUBLIC";
        }
        return userRepository.findByNormalizedUsername(performedBy)
                .map(User::getRole)
                .map(Enum::name)
                .orElse("");
    }

    private User currentActor() {
        var authentication = org.springframework.security.core.context.SecurityContextHolder
                .getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new org.springframework.security.access.AccessDeniedException("Authentication required");
        }
        return userRepository.findByNormalizedUsername(authentication.getName())
                .orElseThrow(() -> new org.springframework.security.access.AccessDeniedException("User not found"));
    }

    private LocalDateTime parseDateTime(String value) {
        if (!hasText(value)) {
            return null;
        }
        String trimmed = value.trim();
        try {
            return LocalDateTime.parse(trimmed);
        } catch (DateTimeParseException ignored) {
            try {
                return LocalDate.parse(trimmed).atStartOfDay();
            } catch (DateTimeParseException ignoredAgain) {
                return null;
            }
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private String like(String value) {
        return "%" + value.trim().toLowerCase(Locale.ROOT) + "%";
    }

}

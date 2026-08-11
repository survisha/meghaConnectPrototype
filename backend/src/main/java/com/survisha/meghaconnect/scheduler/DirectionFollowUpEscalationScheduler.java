package com.survisha.meghaconnect.scheduler;

import com.survisha.meghaconnect.entity.DirectionFollowUp;
import com.survisha.meghaconnect.repository.DirectionFollowUpRepository;
import com.survisha.meghaconnect.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;

@Component
@RequiredArgsConstructor
public class DirectionFollowUpEscalationScheduler {
    private final DirectionFollowUpRepository repository;
    private final AuditLogService auditLogService;

    @Value("${meghaconnect.follow-up.escalation-interval-hours:24}")
    private long escalationIntervalHours;

    @Scheduled(fixedDelayString = "${meghaconnect.follow-up.scheduler-delay-ms:3600000}")
    @Transactional
    public void flagOverdueFollowUps() {
        LocalDateTime now = LocalDateTime.now();
        for (DirectionFollowUp item : repository.findDueForEscalation(DirectionFollowUp.FollowUpStatus.COMPLETED, LocalDate.now(),
                now.minusHours(Math.max(1, escalationIntervalHours)))) {
            item.setLastEscalatedAt(now);
            repository.save(item);
            auditLogService.log("DirectionFollowUp", item.getId(), "FOLLOW_UP_OVERDUE_ESCALATED",
                    "Direction " + item.getDirectionId() + " is overdue for department "
                            + item.getDepartment().getDepartmentName(), "follow-up-scheduler");
        }
    }
}

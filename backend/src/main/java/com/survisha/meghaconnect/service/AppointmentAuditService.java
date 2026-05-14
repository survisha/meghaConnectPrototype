package com.survisha.meghaconnect.service;

import com.survisha.meghaconnect.entity.Appointment;
import com.survisha.meghaconnect.entity.AppointmentAudit;
import com.survisha.meghaconnect.repository.AppointmentAuditRepository;
import com.survisha.meghaconnect.util.DateTimeUtil;
import com.survisha.meghaconnect.util.RequestContextUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AppointmentAuditService {

    private final AppointmentAuditRepository appointmentAuditRepository;

    @Transactional(propagation = Propagation.MANDATORY)
    public void recordStatusChange(Appointment appointment,
                                   Appointment.AppointmentStatus oldStatus,
                                   Appointment.AppointmentStatus newStatus,
                                   String action,
                                   String remarks,
                                   String performedBy,
                                   String performedRole) {
        AppointmentAudit audit = AppointmentAudit.builder()
                .appointment(appointment)
                .oldStatus(oldStatus)
                .newStatus(newStatus)
                .action(action)
                .remarks(RequestContextUtil.sanitizeForLog(remarks))
                .performedBy(performedBy)
                .performedRole(performedRole)
                .requestId(RequestContextUtil.getRequestId())
                .createdAt(DateTimeUtil.nowIST())
                .build();
        appointmentAuditRepository.save(audit);
        log.info("Appointment status audited appointmentId={} action={} oldStatus={} newStatus={}",
                appointment.getId(), action, oldStatus, newStatus);
    }
}

package com.survisha.meghaconnect.repository;

import com.survisha.meghaconnect.entity.AppointmentAudit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AppointmentAuditRepository extends JpaRepository<AppointmentAudit, Long> {

    List<AppointmentAudit> findByAppointment_IdOrderByCreatedAtAsc(Long appointmentId);
}

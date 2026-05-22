package com.survisha.meghaconnect.repository;

import com.survisha.meghaconnect.entity.ScheduleEvent;
import com.survisha.meghaconnect.entity.Appointment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface ScheduleEventRepository extends JpaRepository<ScheduleEvent, Long>, JpaSpecificationExecutor<ScheduleEvent> {

    @Query("SELECT DISTINCT e FROM ScheduleEvent e " +
        "LEFT JOIN FETCH e.appointments a " +
        "LEFT JOIN FETCH a.applicant " +
        "WHERE a IS NULL OR a.status IN :statuses " +
        "ORDER BY e.startTime ASC")
    List<ScheduleEvent> findAllWithAppointments(@Param("statuses") Collection<Appointment.AppointmentStatus> statuses);

    @Query("SELECT DISTINCT e FROM ScheduleEvent e " +
        "LEFT JOIN FETCH e.appointments a " +
        "LEFT JOIN FETCH a.applicant " +
        "WHERE e.startTime < :end AND e.endTime > :start " +
        "AND (a IS NULL OR a.status IN :statuses) " +
        "ORDER BY e.startTime ASC")
    List<ScheduleEvent> findByRangeWithAppointments(@Param("start") LocalDateTime start,
                                                     @Param("end") LocalDateTime end,
                                                     @Param("statuses") Collection<Appointment.AppointmentStatus> statuses);

    @Query("SELECT DISTINCT e FROM ScheduleEvent e " +
        "LEFT JOIN FETCH e.appointments a " +
        "LEFT JOIN FETCH a.applicant " +
        "WHERE e.id = :id " +
        "AND (a IS NULL OR a.status IN :statuses)")
    Optional<ScheduleEvent> findByIdWithAppointments(@Param("id") Long id,
                                                     @Param("statuses") Collection<Appointment.AppointmentStatus> statuses);
}

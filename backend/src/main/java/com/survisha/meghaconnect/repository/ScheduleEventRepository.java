package com.survisha.meghaconnect.repository;

import com.survisha.meghaconnect.entity.ScheduleEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ScheduleEventRepository extends JpaRepository<ScheduleEvent, Long>, JpaSpecificationExecutor<ScheduleEvent> {

    @Query("SELECT DISTINCT e FROM ScheduleEvent e " +
        "LEFT JOIN FETCH e.appointments a " +
        "LEFT JOIN FETCH a.applicant " +
        "ORDER BY e.startTime ASC")
    List<ScheduleEvent> findAllWithAppointments();

    @Query("SELECT DISTINCT e FROM ScheduleEvent e " +
        "LEFT JOIN FETCH e.appointments a " +
        "LEFT JOIN FETCH a.applicant " +
        "WHERE e.startTime < :end AND e.endTime > :start " +
        "ORDER BY e.startTime ASC")
    List<ScheduleEvent> findByRangeWithAppointments(@Param("start") LocalDateTime start,
                                                     @Param("end") LocalDateTime end);
}

package com.survisha.meghaconnect.repository;

import com.survisha.meghaconnect.entity.DirectionFollowUp;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DirectionFollowUpRepository extends JpaRepository<DirectionFollowUp, Long>, JpaSpecificationExecutor<DirectionFollowUp> {
    List<DirectionFollowUp> findByVisitor_IdOrderByCreatedAtDesc(Long visitorId);
    List<DirectionFollowUp> findByAppointment_IdOrderByCreatedAtAsc(Long appointmentId);
    List<DirectionFollowUp> findByAppointment_IdIn(List<Long> appointmentIds);

    @Query("select f from DirectionFollowUp f where f.status <> :completed " +
            "and f.dueDate < :today and (f.lastEscalatedAt is null or f.lastEscalatedAt < :cutoff)")
    List<DirectionFollowUp> findDueForEscalation(@Param("completed") DirectionFollowUp.FollowUpStatus completed,
                                                 @Param("today") LocalDate today,
                                                 @Param("cutoff") LocalDateTime cutoff);
}

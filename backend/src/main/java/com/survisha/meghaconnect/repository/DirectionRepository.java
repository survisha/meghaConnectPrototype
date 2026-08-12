package com.survisha.meghaconnect.repository;

import com.survisha.meghaconnect.entity.Direction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface DirectionRepository extends JpaRepository<Direction, Long>, JpaSpecificationExecutor<Direction> {
    List<Direction> findByAppointment_IdOrderByCreatedAtAsc(Long appointmentId);
}

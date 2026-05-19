package com.survisha.meghaconnect.repository;

import com.survisha.meghaconnect.entity.WalkIn;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WalkInRepository extends JpaRepository<WalkIn, Long> {
    Optional<WalkIn> findByAppointment_Id(Long appointmentId);
}

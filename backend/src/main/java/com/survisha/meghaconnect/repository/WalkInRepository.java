package com.survisha.meghaconnect.repository;

import com.survisha.meghaconnect.entity.WalkIn;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

@Repository
public interface WalkInRepository extends JpaRepository<WalkIn, Long> {
    Optional<WalkIn> findByAppointment_Id(Long appointmentId);
    long countByStatus(WalkIn.WalkInStatus status);
    @Query(value = "SELECT COUNT(*) FROM walkins w JOIN appointments a ON a.id = w.appointment_id WHERE a.status = :status", nativeQuery = true)
    long countByAppointmentStatus(@Param("status") String status);
    @Query(value = "SELECT COUNT(*) FROM walkins w JOIN appointments a ON a.id = w.appointment_id WHERE a.status = :status AND a.tenant_department_id = :departmentId", nativeQuery = true)
    long countByAppointmentStatusAndDepartment(@Param("status") String status, @Param("departmentId") Long departmentId);
}

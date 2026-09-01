package com.survisha.meghaconnect.repository;

import com.survisha.meghaconnect.entity.AssociateMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AssociateMappingRepository extends JpaRepository<AssociateMapping, Long> {
    List<AssociateMapping> findByAppointment_Id(Long appointmentId);
    List<AssociateMapping> findByAppointment_IdOrderByCreatedAtAsc(Long appointmentId);
    List<AssociateMapping> findByPerson_IdOrderByCreatedAtDesc(Long personId);
    @Query(value = "SELECT am.* FROM associate_mappings am " +
        "JOIN appointments a ON a.id = am.appointment_id " +
        "WHERE am.person_id = :personId AND a.status <> 'DUMMY' " +
        "ORDER BY am.created_at DESC", nativeQuery = true)
    List<AssociateMapping> findProductionByPersonIdOrderByCreatedAtDesc(@Param("personId") Long personId);
    boolean existsByAppointment_IdAndPerson_Id(Long appointmentId, Long personId);
}

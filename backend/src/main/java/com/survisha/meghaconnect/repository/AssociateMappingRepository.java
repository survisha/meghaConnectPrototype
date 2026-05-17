package com.survisha.meghaconnect.repository;

import com.survisha.meghaconnect.entity.AssociateMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AssociateMappingRepository extends JpaRepository<AssociateMapping, Long> {
    List<AssociateMapping> findByAppointment_Id(Long appointmentId);
    List<AssociateMapping> findByAppointment_IdOrderByCreatedAtAsc(Long appointmentId);
    List<AssociateMapping> findByPerson_IdOrderByCreatedAtDesc(Long personId);
    boolean existsByAppointment_IdAndPerson_Id(Long appointmentId, Long personId);
}

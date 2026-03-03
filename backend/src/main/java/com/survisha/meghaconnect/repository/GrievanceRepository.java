package com.survisha.meghaconnect.repository;

import com.survisha.meghaconnect.entity.Grievance;
import com.survisha.meghaconnect.entity.Grievance.GrievanceStatus;
import com.survisha.meghaconnect.entity.Grievance.GrievanceCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface GrievanceRepository extends JpaRepository<Grievance, Long>, JpaSpecificationExecutor<Grievance> {
    Optional<Grievance> findByTicketId(String ticketId);
    List<Grievance> findByStatus(GrievanceStatus status);
    List<Grievance> findByCategory(GrievanceCategory category);
    List<Grievance> findByPhoneNumber(String phoneNumber);
}

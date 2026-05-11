package com.survisha.meghaconnect.repository;

import com.survisha.meghaconnect.entity.Grievance;
import com.survisha.meghaconnect.entity.Grievance.GrievanceStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface GrievanceRepository extends JpaRepository<Grievance, Long>, JpaSpecificationExecutor<Grievance> {
    Optional<Grievance> findByTicketId(String ticketId);
    List<Grievance> findByStatus(GrievanceStatus status);

    @Query("SELECT g FROM Grievance g WHERE g.visitor.id = :visitorId")
    Page<Grievance> findByVisitorId(@Param("visitorId") Long visitorId, Pageable pageable);

    @Query("SELECT g FROM Grievance g WHERE g.id = :id AND g.visitor.id = :visitorId")
    Optional<Grievance> findByIdAndVisitorId(@Param("id") Long id, @Param("visitorId") Long visitorId);
}

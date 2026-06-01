package com.survisha.meghaconnect.repository;

import com.survisha.meghaconnect.entity.AiCallAudit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AiCallAuditRepository extends JpaRepository<AiCallAudit, Long> {
}

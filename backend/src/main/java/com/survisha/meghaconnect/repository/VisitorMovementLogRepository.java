package com.survisha.meghaconnect.repository;

import com.survisha.meghaconnect.entity.VisitorMovementLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface VisitorMovementLogRepository extends JpaRepository<VisitorMovementLog, Long>, JpaSpecificationExecutor<VisitorMovementLog> {
}

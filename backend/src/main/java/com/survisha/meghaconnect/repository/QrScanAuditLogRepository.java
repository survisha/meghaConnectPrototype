package com.survisha.meghaconnect.repository;

import com.survisha.meghaconnect.entity.QrScanAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QrScanAuditLogRepository extends JpaRepository<QrScanAuditLog, Long>, JpaSpecificationExecutor<QrScanAuditLog> {
    List<QrScanAuditLog> findTop20ByScannedByOrderByCreatedAtDesc(String scannedBy);
}

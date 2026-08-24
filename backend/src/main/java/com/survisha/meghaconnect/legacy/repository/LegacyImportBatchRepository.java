package com.survisha.meghaconnect.legacy.repository;
import com.survisha.meghaconnect.legacy.entity.LegacyImportBatch;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;
public interface LegacyImportBatchRepository extends JpaRepository<LegacyImportBatch,Long> {
    Page<LegacyImportBatch> findByUploadedByOrderByUploadedAtDesc(String uploadedBy, Pageable pageable);
    Page<LegacyImportBatch> findAllByOrderByUploadedAtDesc(Pageable pageable);
}

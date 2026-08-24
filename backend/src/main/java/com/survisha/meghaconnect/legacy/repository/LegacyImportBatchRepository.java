package com.survisha.meghaconnect.legacy.repository;
import com.survisha.meghaconnect.legacy.entity.LegacyImportBatch;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import javax.persistence.LockModeType;
import java.util.Optional;
public interface LegacyImportBatchRepository extends JpaRepository<LegacyImportBatch,Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select batch from LegacyImportBatch batch where batch.id = :id")
    Optional<LegacyImportBatch> findByIdForUpdate(@Param("id") Long id);
    Page<LegacyImportBatch> findByUploadedByOrderByUploadedAtDesc(String uploadedBy, Pageable pageable);
    Page<LegacyImportBatch> findAllByOrderByUploadedAtDesc(Pageable pageable);
}

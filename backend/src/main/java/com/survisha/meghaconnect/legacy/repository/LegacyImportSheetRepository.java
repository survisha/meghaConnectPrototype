package com.survisha.meghaconnect.legacy.repository;
import com.survisha.meghaconnect.legacy.entity.LegacyImportSheet;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface LegacyImportSheetRepository extends JpaRepository<LegacyImportSheet,Long> {
    List<LegacyImportSheet> findByBatchIdOrderBySheetIndex(Long batchId);
    Optional<LegacyImportSheet> findByIdAndBatchId(Long id, Long batchId);
}

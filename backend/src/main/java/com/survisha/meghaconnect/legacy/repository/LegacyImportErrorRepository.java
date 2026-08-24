package com.survisha.meghaconnect.legacy.repository;
import com.survisha.meghaconnect.legacy.entity.LegacyImportError;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;
public interface LegacyImportErrorRepository extends JpaRepository<LegacyImportError,Long> {
    Page<LegacyImportError> findByImportBatchIdOrderByImportSheetIdAscSourceRowNumberAsc(Long batchId, Pageable pageable);
    void deleteByImportSheetId(Long sheetId);
}

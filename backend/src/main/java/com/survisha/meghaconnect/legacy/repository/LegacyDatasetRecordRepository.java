package com.survisha.meghaconnect.legacy.repository;
import com.survisha.meghaconnect.legacy.entity.LegacyDatasetRecord;
import org.springframework.data.jpa.repository.JpaRepository;
public interface LegacyDatasetRecordRepository extends JpaRepository<LegacyDatasetRecord,Long> {
    boolean existsByDatasetDefinitionIdAndRecordFingerprint(Long datasetId, String fingerprint);
    boolean existsByImportBatchIdAndSourceSheetAndSourceRowNumberAndDatasetDefinitionId(Long batchId, String sheet, long row, Long datasetId);
}

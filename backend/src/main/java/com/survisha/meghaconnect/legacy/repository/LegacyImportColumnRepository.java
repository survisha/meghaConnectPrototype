package com.survisha.meghaconnect.legacy.repository;
import com.survisha.meghaconnect.legacy.entity.LegacyImportColumn;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface LegacyImportColumnRepository extends JpaRepository<LegacyImportColumn,Long> {
    List<LegacyImportColumn> findBySheetIdOrderBySourceColumnIndex(Long sheetId);
    void deleteBySheetId(Long sheetId);
}

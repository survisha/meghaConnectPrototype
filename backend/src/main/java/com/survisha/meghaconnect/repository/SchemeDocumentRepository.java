package com.survisha.meghaconnect.repository;

import com.survisha.meghaconnect.entity.SchemeDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SchemeDocumentRepository extends JpaRepository<SchemeDocument, Long> {

    @Query("SELECT sd FROM SchemeDocument sd WHERE sd.schemeCode = :schemeCode ORDER BY sd.displayOrder")
    List<SchemeDocument> findBySchemeCode(@Param("schemeCode") String schemeCode);

    @Query("SELECT sd FROM SchemeDocument sd WHERE sd.schemeCode = :schemeCode AND sd.isRequired = true")
    List<SchemeDocument> findRequiredBySchemeCode(@Param("schemeCode") String schemeCode);

    @Query("DELETE FROM SchemeDocument sd WHERE sd.schemeCode = :schemeCode")
    void deleteBySchemeCode(@Param("schemeCode") String schemeCode);
}

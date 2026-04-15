package com.survisha.meghaconnect.repository;

import com.survisha.meghaconnect.entity.ReferenceData;
import com.survisha.meghaconnect.entity.ReferenceType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReferenceDataRepository extends JpaRepository<ReferenceData, Long> {

    List<ReferenceData> findByTypeAndIsActiveOrderByDisplayOrder(ReferenceType type, Boolean isActive);

    List<ReferenceData> findByTypeCodeAndIsActiveOrderByDisplayOrder(String typeCode, Boolean isActive);

    @Query("SELECT rd FROM ReferenceData rd WHERE rd.type.code = :typeCode AND rd.isActive = true ORDER BY rd.displayOrder")
    List<ReferenceData> findActiveByTypeCode(@Param("typeCode") String typeCode);

    boolean existsByTypeAndCode(ReferenceType type, String code);
}
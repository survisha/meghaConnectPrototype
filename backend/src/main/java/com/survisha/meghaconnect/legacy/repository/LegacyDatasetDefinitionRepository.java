package com.survisha.meghaconnect.legacy.repository;
import com.survisha.meghaconnect.legacy.entity.LegacyDatasetDefinition;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface LegacyDatasetDefinitionRepository extends JpaRepository<LegacyDatasetDefinition,Long> {
    List<LegacyDatasetDefinition> findByActiveTrueAndApprovedTrueOrderByDatasetNameAsc();
    Optional<LegacyDatasetDefinition> findByDatasetCodeIgnoreCase(String code);
}

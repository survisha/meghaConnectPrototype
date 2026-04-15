package com.survisha.meghaconnect.repository;

import com.survisha.meghaconnect.entity.AppointmentTypeConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AppointmentTypeConfigRepository extends JpaRepository<AppointmentTypeConfig, Long> {

    Optional<AppointmentTypeConfig> findByTypeCode(String typeCode);

    @Query("SELECT a FROM AppointmentTypeConfig a WHERE a.isActive = true ORDER BY a.displayOrder ASC")
    List<AppointmentTypeConfig> findAllActive();

    @Query("SELECT a FROM AppointmentTypeConfig a WHERE a.typeCategory = :category AND a.isActive = true ORDER BY a.displayOrder ASC")
    List<AppointmentTypeConfig> findByTypeCategory(@Param("category") String typeCategory);

    @Query("SELECT COALESCE(MAX(a.displayOrder), 0) FROM AppointmentTypeConfig a")
    Optional<Integer> findMaxDisplayOrder();

    @Query("SELECT a FROM AppointmentTypeConfig a WHERE a.requiresTravel = true AND a.isActive = true ORDER BY a.displayOrder ASC")
    List<AppointmentTypeConfig> findTravelRequiredTypes();

    @Query("SELECT a FROM AppointmentTypeConfig a WHERE a.hasAppointmentLimit = true AND a.isActive = true ORDER BY a.displayOrder ASC")
    List<AppointmentTypeConfig> findTypesWithLimits();
}

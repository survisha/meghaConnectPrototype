package com.survisha.meghaconnect.service;

import com.survisha.meghaconnect.dto.AppointmentTypeDto;
import com.survisha.meghaconnect.entity.AppointmentTypeConfig;
import com.survisha.meghaconnect.repository.AppointmentTypeConfigRepository;
import com.survisha.meghaconnect.security.JwtUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AppointmentTypeService {

    private final com.survisha.meghaconnect.security.AccessPolicy accessPolicy;

    private final AppointmentTypeConfigRepository appointmentTypeConfigRepository;
    private final JwtUtils jwtUtils;

    /**
     * Get all appointment type configurations
     */
    public List<AppointmentTypeDto> getAllAppointmentTypes() {
        log.debug("Fetching all appointment type configurations");
        return appointmentTypeConfigRepository.findAllActive()
            .stream()
            .map(this::convertToDto)
            .collect(Collectors.toList());
    }

    /**
     * Get appointment type by code
     */
    public Optional<AppointmentTypeDto> getAppointmentTypeByCode(String typeCode) {
        log.debug("Fetching appointment type: {}", typeCode);
        return appointmentTypeConfigRepository.findByTypeCode(typeCode)
            .map(this::convertToDto);
    }

    /**
     * Get appointment types by category (INDIVIDUAL, BATCH)
     */
    public List<AppointmentTypeDto> getAppointmentTypesByCategory(String category) {
        log.debug("Fetching appointment types for category: {}", category);
        return appointmentTypeConfigRepository.findByTypeCategory(category)
            .stream()
            .map(this::convertToDto)
            .collect(Collectors.toList());
    }

    /**
     * Create new appointment type configuration
     */
    public AppointmentTypeDto createAppointmentType(AppointmentTypeDto dto, HttpServletRequest request) {
        log.info("Creating new appointment type: {}", dto.getTypeCode());

        Optional<AppointmentTypeConfig> existing = appointmentTypeConfigRepository.findByTypeCode(dto.getTypeCode());
        if (existing.isPresent()) {
            throw new IllegalArgumentException("Appointment type code already exists: " + dto.getTypeCode());
        }

        String username = jwtUtils.getUsernameFromRequest(request);

        Integer maxOrder = appointmentTypeConfigRepository.findMaxDisplayOrder().orElse(0);

        AppointmentTypeConfig config = AppointmentTypeConfig.builder()
            .typeCode(dto.getTypeCode())
            .typeName(dto.getTypeName())
            .description(dto.getDescription())
            .typeCategory(dto.getTypeCategory())
            .requiresTravel(dto.getRequiresTravel() != null ? dto.getRequiresTravel() : false)
            .travelTimeBefore(dto.getTravelTimeBefore() != null ? dto.getTravelTimeBefore() : 0)
            .travelTimeAfter(dto.getTravelTimeAfter() != null ? dto.getTravelTimeAfter() : 0)
            .blockTimeIncludes(dto.getBlockTimeIncludes() != null ? dto.getBlockTimeIncludes() : true)
            .hasAppointmentLimit(dto.getHasAppointmentLimit() != null ? dto.getHasAppointmentLimit() : false)
            .maxAppointmentLimit(dto.getMaxAppointmentLimit())
            .limitIsSacrosanct(dto.getLimitIsSacrosanct() != null ? dto.getLimitIsSacrosanct() : true)
            .generateAlerts(dto.getGenerateAlerts() != null ? dto.getGenerateAlerts() : false)
            .noTravelTime(dto.getNoTravelTime() != null ? dto.getNoTravelTime() : false)
            .isActive(true)
            .displayOrder(maxOrder + 1)
            .createdBy(username)
            .updatedBy(username)
            .build();

        AppointmentTypeConfig saved = appointmentTypeConfigRepository.save(config);
        log.info("Appointment type created: {} by {}", saved.getTypeCode(), username);

        return convertToDto(saved);
    }

    /**
     * Update appointment type configuration
     */
    public AppointmentTypeDto updateAppointmentType(String typeCode, AppointmentTypeDto dto, HttpServletRequest request) {
        log.info("Updating appointment type: {}", typeCode);

        Optional<AppointmentTypeConfig> existing = appointmentTypeConfigRepository.findByTypeCode(typeCode);
        if (!existing.isPresent()) {
            throw new IllegalArgumentException("Appointment type not found: " + typeCode);
        }

        String username = jwtUtils.getUsernameFromRequest(request);
        AppointmentTypeConfig config = existing.get();

        // Update fields
        if (dto.getTypeName() != null) config.setTypeName(dto.getTypeName());
        if (dto.getDescription() != null) config.setDescription(dto.getDescription());
        if (dto.getRequiresTravel() != null) config.setRequiresTravel(dto.getRequiresTravel());
        if (dto.getTravelTimeBefore() != null) config.setTravelTimeBefore(dto.getTravelTimeBefore());
        if (dto.getTravelTimeAfter() != null) config.setTravelTimeAfter(dto.getTravelTimeAfter());
        if (dto.getBlockTimeIncludes() != null) config.setBlockTimeIncludes(dto.getBlockTimeIncludes());
        if (dto.getHasAppointmentLimit() != null) config.setHasAppointmentLimit(dto.getHasAppointmentLimit());
        if (dto.getMaxAppointmentLimit() != null) config.setMaxAppointmentLimit(dto.getMaxAppointmentLimit());
        if (dto.getLimitIsSacrosanct() != null) config.setLimitIsSacrosanct(dto.getLimitIsSacrosanct());
        if (dto.getGenerateAlerts() != null) config.setGenerateAlerts(dto.getGenerateAlerts());
        if (dto.getNoTravelTime() != null) config.setNoTravelTime(dto.getNoTravelTime());
        if (dto.getIsActive() != null) config.setIsActive(dto.getIsActive());

        config.setUpdatedBy(username);

        AppointmentTypeConfig updated = appointmentTypeConfigRepository.save(config);
        log.info("Appointment type updated: {} by {}", updated.getTypeCode(), username);

        return convertToDto(updated);
    }

    /**
     * Toggle active status of appointment type
     */
    public AppointmentTypeDto toggleAppointmentTypeStatus(String typeCode, HttpServletRequest request) {
        log.info("Toggling status for appointment type: {}", typeCode);

        Optional<AppointmentTypeConfig> existing = appointmentTypeConfigRepository.findByTypeCode(typeCode);
        if (!existing.isPresent()) {
            throw new IllegalArgumentException("Appointment type not found: " + typeCode);
        }

        String username = jwtUtils.getUsernameFromRequest(request);
        AppointmentTypeConfig config = existing.get();

        config.setIsActive(!config.getIsActive());
        config.setUpdatedBy(username);

        AppointmentTypeConfig updated = appointmentTypeConfigRepository.save(config);
        log.info("Appointment type status toggled: {} - IsActive: {} by {}", 
            updated.getTypeCode(), updated.getIsActive(), username);

        return convertToDto(updated);
    }

    /**
     * Check if user has ADMIN role
     */
    public boolean isAdminUser(HttpServletRequest request) {
        return accessPolicy.canManageCmoConfiguration();
    }

    /**
     * Convert AppointmentTypeConfig entity to DTO
     */
    private AppointmentTypeDto convertToDto(AppointmentTypeConfig config) {
        return AppointmentTypeDto.builder()
            .id(config.getId())
            .typeCode(config.getTypeCode())
            .typeName(config.getTypeName())
            .description(config.getDescription())
            .typeCategory(config.getTypeCategory())
            .requiresTravel(config.getRequiresTravel())
            .travelTimeBefore(config.getTravelTimeBefore())
            .travelTimeAfter(config.getTravelTimeAfter())
            .blockTimeIncludes(config.getBlockTimeIncludes())
            .hasAppointmentLimit(config.getHasAppointmentLimit())
            .maxAppointmentLimit(config.getMaxAppointmentLimit())
            .limitIsSacrosanct(config.getLimitIsSacrosanct())
            .generateAlerts(config.getGenerateAlerts())
            .noTravelTime(config.getNoTravelTime())
            .isActive(config.getIsActive())
            .displayOrder(config.getDisplayOrder())
            .createdBy(config.getCreatedBy())
            .updatedBy(config.getUpdatedBy())
            .build();
    }
}

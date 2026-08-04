package com.survisha.meghaconnect.service;

import com.survisha.meghaconnect.dto.ReferenceDataDto;
import com.survisha.meghaconnect.entity.ReferenceData;
import com.survisha.meghaconnect.repository.ReferenceDataRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import com.survisha.meghaconnect.monitoring.MonitoredOperation;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReferenceDataService {

    private final ReferenceDataRepository referenceDataRepository;

    @Cacheable(value = "referenceData", key = "#typeCode")
    @MonitoredOperation(value = "reference_data_lookup", category = MonitoredOperation.Category.DATABASE)
    public List<ReferenceDataDto> getReferenceDataByType(String typeCode) {
        log.debug("Fetching reference data for type: {}", typeCode);

        List<ReferenceData> data = referenceDataRepository.findActiveByTypeCode(typeCode);

        return data.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Cacheable(value = "referenceData", key = "#typeCode + ':' + #parentCode")
    @MonitoredOperation(value = "reference_data_lookup", category = MonitoredOperation.Category.DATABASE)
    public List<ReferenceDataDto> getReferenceDataByType(String typeCode, String parentCode) {
        String normalizedType = typeCode.toUpperCase(Locale.ROOT);
        if (parentCode == null || parentCode.isBlank()) {
            return getReferenceDataByType(normalizedType);
        }
        ReferenceData parent = referenceDataRepository
                .findByTypeCodeAndCodeAndIsActive("MEGHALAYA_DISTRICT", parentCode.toUpperCase(Locale.ROOT), true)
                .orElseThrow(() -> new IllegalArgumentException("Active Meghalaya district not found"));
        return referenceDataRepository
                .findByTypeCodeAndParentAndIsActiveOrderByDisplayOrder(normalizedType, parent, true)
                .stream().map(this::convertToDto).collect(Collectors.toList());
    }

    private ReferenceDataDto convertToDto(ReferenceData data) {
        return ReferenceDataDto.builder()
                .code(data.getCode())
                .value(data.getValue())
                .build();
    }
}

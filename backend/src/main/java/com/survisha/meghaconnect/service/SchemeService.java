package com.survisha.meghaconnect.service;

import com.survisha.meghaconnect.dto.SchemeDto;
import com.survisha.meghaconnect.dto.SchemeDocumentDto;
import com.survisha.meghaconnect.entity.ReferenceData;
import com.survisha.meghaconnect.entity.ReferenceType;
import com.survisha.meghaconnect.entity.SchemeDocument;
import com.survisha.meghaconnect.repository.ReferenceDataRepository;
import com.survisha.meghaconnect.repository.ReferenceTypeRepository;
import com.survisha.meghaconnect.repository.SchemeDocumentRepository;
import com.survisha.meghaconnect.security.JwtUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class SchemeService {

    private static final String CM_SCHEME_TYPE = "CM_SCHEME";

    private final ReferenceDataRepository referenceDataRepository;
    private final ReferenceTypeRepository referenceTypeRepository;
    private final SchemeDocumentRepository schemeDocumentRepository;
    private final JwtUtils jwtUtils;

    /**
     * Get all CM schemes from reference_data table
     */
    public List<SchemeDto> getAllSchemes() {
        log.debug("Fetching all schemes from reference_data");
        
        Optional<ReferenceType> refType = referenceTypeRepository.findByCode(CM_SCHEME_TYPE);
        if (!refType.isPresent()) {
            log.warn("CM_SCHEME reference type not found");
            return Collections.emptyList();
        }

        List<ReferenceData> schemes = referenceDataRepository.findByTypeOrderByDisplayOrder(refType.get());
        return schemes.stream()
            .map(this::convertRefDataToDto)
            .collect(Collectors.toList());
    }

    /**
     * Get scheme by code with its required documents
     */
    public Optional<SchemeDto> getSchemeByCode(String schemeCode) {
        log.debug("Fetching scheme: {}", schemeCode);
        
        Optional<ReferenceType> refType = referenceTypeRepository.findByCode(CM_SCHEME_TYPE);
        if (!refType.isPresent()) {
            log.warn("CM_SCHEME reference type not found");
            return Optional.empty();
        }

        return referenceDataRepository.findByTypeAndCode(refType.get(), schemeCode)
            .map(this::convertRefDataToDto);
    }

    /**
     * Create new scheme in reference_data table
     */
    public SchemeDto createScheme(SchemeDto schemeDto, HttpServletRequest request) {
        log.info("Creating new scheme: {}", schemeDto.getSchemeCode());
        
        Optional<ReferenceType> refType = referenceTypeRepository.findByCode(CM_SCHEME_TYPE);
        if (!refType.isPresent()) {
            throw new RuntimeException("CM_SCHEME reference type not found");
        }

        // Check if scheme code already exists
        Optional<ReferenceData> existing = referenceDataRepository.findByTypeAndCode(refType.get(), schemeDto.getSchemeCode());
        if (existing.isPresent()) {
            throw new IllegalArgumentException("Scheme code already exists: " + schemeDto.getSchemeCode());
        }

        String username = jwtUtils.getUsernameFromRequest(request);

        // Get max display order
        Integer maxOrder = referenceDataRepository.findMaxDisplayOrder(refType.get())
            .orElse(0);

        ReferenceData scheme = ReferenceData.builder()
            .type(refType.get())
            .code(schemeDto.getSchemeCode())
            .value(schemeDto.getSchemeName())
            .displayOrder(maxOrder + 1)
            .isActive(true)
            .build();

        ReferenceData savedScheme = referenceDataRepository.save(scheme);

        log.info("Scheme created: {} by {}", savedScheme.getCode(), username);

        return convertRefDataToDto(savedScheme);
    }

    /**
     * Update scheme active/inactive status and name
     */
    public SchemeDto updateScheme(String schemeCode, SchemeDto schemeDto, HttpServletRequest request) {
        log.info("Updating scheme: {}", schemeCode);
        
        Optional<ReferenceType> refType = referenceTypeRepository.findByCode(CM_SCHEME_TYPE);
        if (!refType.isPresent()) {
            throw new RuntimeException("CM_SCHEME reference type not found");
        }

        Optional<ReferenceData> scheme = referenceDataRepository.findByTypeAndCode(refType.get(), schemeCode);
        if (!scheme.isPresent()) {
            throw new IllegalArgumentException("Scheme not found: " + schemeCode);
        }

        String username = jwtUtils.getUsernameFromRequest(request);
        ReferenceData refData = scheme.get();

        // Update fields
        if (schemeDto.getSchemeName() != null) {
            refData.setValue(schemeDto.getSchemeName());
        }
        if (schemeDto.getIsActive() != null) {
            refData.setIsActive(schemeDto.getIsActive());
            
            // Log active/inactive change
            if (!refData.getIsActive()) {
                log.info("Scheme marked inactive: {} by {}", refData.getCode(), username);
            } else {
                log.info("Scheme marked active: {} by {}", refData.getCode(), username);
            }
        }

        ReferenceData updatedScheme = referenceDataRepository.save(refData);

        return convertRefDataToDto(updatedScheme);
    }

    /**
     * Configure required documents for a scheme
     */
    public SchemeDto configureSchemeDocuments(String schemeCode, List<SchemeDocumentDto> documentDtos, HttpServletRequest request) {
        log.info("Configuring documents for scheme: {}", schemeCode);
        
        Optional<ReferenceType> refType = referenceTypeRepository.findByCode(CM_SCHEME_TYPE);
        if (!refType.isPresent()) {
            throw new RuntimeException("CM_SCHEME reference type not found");
        }

        Optional<ReferenceData> scheme = referenceDataRepository.findByTypeAndCode(refType.get(), schemeCode);
        if (!scheme.isPresent()) {
            throw new IllegalArgumentException("Scheme not found: " + schemeCode);
        }

        String username = jwtUtils.getUsernameFromRequest(request);

        // Delete existing documents for this scheme
        schemeDocumentRepository.deleteBySchemeCode(schemeCode);

        // Add new documents
        List<SchemeDocument> documents = documentDtos.stream()
            .map(dto -> {
                SchemeDocument doc = new SchemeDocument();
                doc.setSchemeCode(schemeCode);
                doc.setDocumentType(dto.getDocumentType());
                doc.setDocumentLabel(dto.getDocumentLabel());
                doc.setIsRequired(dto.getIsRequired() != null ? dto.getIsRequired() : true);
                doc.setDescription(dto.getDescription());
                doc.setFileFormatAllowed(dto.getFileFormatAllowed());
                doc.setDisplayOrder(dto.getDisplayOrder());
                doc.setCreatedBy(username);
                doc.setUpdatedBy(username);
                return doc;
            })
            .collect(Collectors.toList());

        schemeDocumentRepository.saveAll(documents);

        log.info("Documents configured for scheme: {} by {}", schemeCode, username);

        // Reload scheme with new documents
        ReferenceData refData = scheme.get();

        return convertRefDataToDto(refData);
    }

    /**
     * Check if user has ADMIN role
     */
    public boolean isAdminUser(HttpServletRequest request) {
        try {
            String token = jwtUtils.extractTokenFromRequest(request);
            if (token == null) {
                return false;
            }

            String role = jwtUtils.getRoleFromToken(token);
            return "ADMIN".equals(role);
        } catch (Exception e) {
            log.error("Error checking admin role", e);
            return false;
        }
    }

    /**
     * Convert ReferenceData (scheme) to SchemeDto
     */
    private SchemeDto convertRefDataToDto(ReferenceData refData) {
        List<SchemeDocumentDto> documentDtos = schemeDocumentRepository.findBySchemeCode(refData.getCode())
            .stream()
            .map(doc -> SchemeDocumentDto.builder()
                .id(doc.getId())
                .documentType(doc.getDocumentType())
                .documentLabel(doc.getDocumentLabel())
                .isRequired(doc.getIsRequired())
                .description(doc.getDescription())
                .fileFormatAllowed(doc.getFileFormatAllowed())
                .displayOrder(doc.getDisplayOrder())
                .createdBy(doc.getCreatedBy())
                .updatedBy(doc.getUpdatedBy())
                .build())
            .collect(Collectors.toList());

        return SchemeDto.builder()
            .id(refData.getId())
            .schemeCode(refData.getCode())
            .schemeName(refData.getValue())
            .description(null) // Description not stored in reference_data, can be extended if needed
            .isActive(refData.getIsActive())
            .requiredDocuments(documentDtos)
            .createdBy(null)
            .updatedBy(null)
            .build();
    }
}

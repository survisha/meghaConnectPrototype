package com.survisha.meghaconnect.service;

import com.survisha.meghaconnect.dto.CreateSchemeApplicationRequest;
import com.survisha.meghaconnect.dto.SchemeApplicationDto;
import com.survisha.meghaconnect.dto.SchemeApplicationItemDto;
import com.survisha.meghaconnect.dto.VisitorDto;
import com.survisha.meghaconnect.entity.Appointment;
import com.survisha.meghaconnect.entity.SchemeApplication;
import com.survisha.meghaconnect.entity.SchemeApplicationItem;
import com.survisha.meghaconnect.entity.Visitor;
import com.survisha.meghaconnect.exception.ErrorCodeConstants;
import com.survisha.meghaconnect.exception.MeghaConnectException;
import com.survisha.meghaconnect.exception.RequestValidationException;
import com.survisha.meghaconnect.exception.VisitorNotFoundException;
import com.survisha.meghaconnect.repository.SchemeApplicationRepository;
import com.survisha.meghaconnect.repository.VisitorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SchemeApplicationService {

    private static final Set<String> DUPLICATE_ALLOWED_FINAL_SCHEME_STATUSES = Set.of(
            "REJECTED",
            "HCM_REJECTED",
            "CANCELLED",
            "CANCELED",
            "COMPLETED",
            "CLOSED"
    );

    private final SchemeApplicationRepository schemeApplicationRepository;
    private final VisitorRepository visitorRepository;
    private final RequestValidationService validationService;
    private final AuditLogService auditLogService;

    @Transactional
    public SchemeApplicationDto create(CreateSchemeApplicationRequest request, String actor) {
        CreateSchemeApplicationRequest safeRequest = request != null ? request : new CreateSchemeApplicationRequest();
        Long applicantId = resolveApplicantId(safeRequest.getApplicantId(), actor);
        Visitor applicant = visitorRepository.findById(applicantId)
                .orElseThrow(() -> new VisitorNotFoundException(applicantId));

        SchemeApplication.SchemeType schemeType = parseSchemeType(safeRequest.getSchemeType());
        ensureSchemeApplicationIsNotDuplicate(applicant, schemeType);

        SchemeApplication application = SchemeApplication.builder()
                .applicant(applicant)
                .appointment(null)
                .schemeType(schemeType)
                .projectName(validationService.requireText(safeRequest.getProjectName(), "projectName"))
                .projectCategory(trimToNull(safeRequest.getProjectCategory()))
                .beneficiaryType(trimToNull(safeRequest.getBeneficiaryType()))
                .beneficiaryCount(trimToNull(safeRequest.getBeneficiaryCount()))
                .estimatedCost(safeRequest.getEstimatedCost())
                .communityContribution(safeRequest.getCommunityContribution())
                .justification(trimToNull(safeRequest.getJustification()))
                .status(Appointment.AppointmentStatus.SUBMITTED.name())
                .build();
        application.setCreatedBy(actor);
        application.setUpdatedBy(actor);

        List<SchemeApplicationItem> items = mapItems(safeRequest.getItems(), application);
        if (!items.isEmpty()) {
            application.setItems(items);
        }

        SchemeApplication saved = schemeApplicationRepository.save(application);
        auditLogService.log("SchemeApplication", saved.getId(), "SUBMITTED",
                "Scheme application submitted directly: " + formatSchemeType(schemeType), actor);
        return toDto(saved);
    }

    public Page<SchemeApplicationDto> findAll(String status, Pageable pageable) {
        Specification<SchemeApplication> spec = (root, query, cb) -> cb.conjunction();
        if (hasText(status)) {
            String normalizedStatus = status.trim().toUpperCase(Locale.ROOT);
            spec = spec.and((root, query, cb) -> cb.equal(cb.upper(root.get("status")), normalizedStatus));
        }
        return schemeApplicationRepository.findAll(spec, pageable).map(this::toDto);
    }

    public List<SchemeApplicationDto> findByVisitor(Long visitorId) {
        if (visitorId == null || visitorId <= 0) {
            throw new RequestValidationException(
                    ErrorCodeConstants.MISSING_REQUIRED_FIELD,
                    ErrorCodeConstants.format(ErrorCodeConstants.MISSING_REQUIRED_FIELD_MSG, "visitorId")
            );
        }
        return schemeApplicationRepository.findByApplicant_IdOrderByCreatedAtDesc(visitorId)
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional
    public SchemeApplicationDto updateStatus(Long id, String status, String remarks, BigDecimal hcmApprovedCost, String actor) {
        SchemeApplication application = schemeApplicationRepository.findById(id)
                .orElseThrow(() -> new MeghaConnectException(
                        ErrorCodeConstants.GENERAL_ERROR,
                        "Scheme application not found with id: " + id,
                        404
                ));
        String normalizedStatus = validationService.requireText(status, "status").toUpperCase(Locale.ROOT);
        application.setStatus(normalizedStatus);
        if (hasText(remarks)) {
            application.setHcmRemarks(remarks.trim());
        }
        if (hcmApprovedCost != null) {
            application.setHcmApprovedCost(hcmApprovedCost);
        }
        application.setUpdatedBy(actor);
        SchemeApplication saved = schemeApplicationRepository.save(application);
        auditLogService.log("SchemeApplication", saved.getId(), "STATUS_CHANGE",
                "Status updated to: " + normalizedStatus, actor);
        return toDto(saved);
    }

    public SchemeApplicationDto toDto(SchemeApplication application) {
        if (application == null) {
            return null;
        }
        Visitor applicant = application.getApplicant();
        return SchemeApplicationDto.builder()
                .id(application.getId())
                .applicantId(applicant != null ? applicant.getId() : null)
                .applicant(applicant != null ? toVisitorDto(applicant) : null)
                .applicantName(applicant != null ? applicant.getFullName() : null)
                .appointmentId(application.getAppointment() != null ? application.getAppointment().getId() : null)
                .schemeType(application.getSchemeType())
                .projectName(application.getProjectName())
                .projectCategory(application.getProjectCategory())
                .beneficiaryType(application.getBeneficiaryType())
                .beneficiaryCount(application.getBeneficiaryCount())
                .estimatedCost(application.getEstimatedCost())
                .communityContribution(application.getCommunityContribution())
                .justification(application.getJustification())
                .hcmDecision(application.getHcmDecision())
                .hcmApprovedCost(application.getHcmApprovedCost())
                .status(application.getStatus())
                .items(application.getItems() != null
                        ? application.getItems().stream().map(this::toItemDto).toList()
                        : List.of())
                .createdAt(application.getCreatedAt())
                .updatedAt(application.getUpdatedAt())
                .build();
    }

    private Long resolveApplicantId(Long applicantId, String actor) {
        Long visitorPrincipalId = visitorIdFromActor(actor);
        if (visitorPrincipalId != null) {
            if (applicantId != null && !visitorPrincipalId.equals(applicantId)) {
                throw new MeghaConnectException(
                        ErrorCodeConstants.INSUFFICIENT_PERMISSIONS,
                        "You can submit scheme applications only for your own visitor profile.",
                        403
                );
            }
            return visitorPrincipalId;
        }
        if (applicantId == null || applicantId <= 0) {
            throw new RequestValidationException(
                    ErrorCodeConstants.MISSING_REQUIRED_FIELD,
                    ErrorCodeConstants.format(ErrorCodeConstants.MISSING_REQUIRED_FIELD_MSG, "applicantId")
            );
        }
        return applicantId;
    }

    private List<SchemeApplicationItem> mapItems(List<SchemeApplicationItemDto> requestItems,
                                                SchemeApplication application) {
        if (requestItems == null) {
            return List.of();
        }
        return requestItems.stream()
                .filter(item -> item != null && hasText(item.getDescription()))
                .map(item -> SchemeApplicationItem.builder()
                        .schemeApplication(application)
                        .description(item.getDescription().trim())
                        .quantity(item.getQuantity() != null && item.getQuantity() > 0 ? item.getQuantity() : 1)
                        .unitCost(item.getUnitCost() != null ? item.getUnitCost() : BigDecimal.ZERO)
                        .build())
                .toList();
    }

    private SchemeApplicationItemDto toItemDto(SchemeApplicationItem item) {
        return SchemeApplicationItemDto.builder()
                .id(item.getId())
                .description(item.getDescription())
                .quantity(item.getQuantity())
                .unitCost(item.getUnitCost())
                .cmoModeratedUnitCost(item.getCmoModeratedUnitCost())
                .hcmApprovedUnitCost(item.getHcmApprovedUnitCost())
                .build();
    }

    private VisitorDto toVisitorDto(Visitor visitor) {
        return VisitorDto.builder()
                .id(visitor.getId())
                .fullName(visitor.getFullName())
                .phoneNumber(visitor.getPhoneNumber())
                .epicNumber(visitor.getEpicNumber())
                .designation(visitor.getDesignation())
                .district(visitor.getDistrict())
                .constituency(visitor.getConstituency())
                .booth(visitor.getBooth())
                .address(visitor.getAddress())
                .fullAddress(visitor.getFullAddress())
                .build();
    }

    private void ensureSchemeApplicationIsNotDuplicate(Visitor applicant, SchemeApplication.SchemeType schemeType) {
        schemeApplicationRepository.findByApplicant_IdAndSchemeTypeOrderByCreatedAtDesc(applicant.getId(), schemeType)
                .stream()
                .filter(this::isActiveSchemeApplication)
                .findFirst()
                .ifPresent(existing -> {
                    String applicationRef = existing.getAppointment() != null
                            ? existing.getAppointment().getApplicationId()
                            : "scheme application #" + existing.getId();
                    String status = hasText(existing.getStatus()) ? existing.getStatus() : "SUBMITTED";
                    throw new MeghaConnectException(
                            ErrorCodeConstants.DUPLICATE_ENTRY,
                            "Multiple applications for " + formatSchemeType(schemeType)
                                    + " are not allowed. Existing application " + applicationRef
                                    + " is currently " + status + ".",
                            409
                    );
                });
    }

    private boolean isActiveSchemeApplication(SchemeApplication application) {
        if (!hasText(application.getStatus())) {
            return true;
        }
        return !DUPLICATE_ALLOWED_FINAL_SCHEME_STATUSES.contains(application.getStatus().trim().toUpperCase(Locale.ROOT));
    }

    private SchemeApplication.SchemeType parseSchemeType(String schemeType) {
        String normalized = normalizeSchemeType(schemeType);
        if (normalized == null) {
            throw new RequestValidationException(
                    ErrorCodeConstants.MISSING_REQUIRED_FIELD,
                    ErrorCodeConstants.format(ErrorCodeConstants.MISSING_REQUIRED_FIELD_MSG, "schemeType")
            );
        }
        try {
            return SchemeApplication.SchemeType.valueOf(normalized);
        } catch (IllegalArgumentException e) {
            throw new RequestValidationException(
                    ErrorCodeConstants.INVALID_FIELD_VALUE,
                    "Invalid scheme type: " + schemeType
            );
        }
    }

    private String normalizeSchemeType(String schemeType) {
        if (!hasText(schemeType)) {
            return null;
        }
        String normalized = schemeType.trim()
                .toUpperCase(Locale.ROOT)
                .replace("&", "AND")
                .replace("+", "_PLUS")
                .replaceAll("[\\s-]+", "_")
                .replaceAll("_+", "_");

        if ("CMCARE".equals(normalized)) {
            return "CM_CARE";
        }
        if ("CMCONNECT".equals(normalized)) {
            return "CM_CONNECT";
        }
        if ("CMELEVATE".equals(normalized)) {
            return "CM_ELEVATE";
        }
        if ("FOCUSPLUS".equals(normalized) || "FOCUS_PLUS".equals(normalized)) {
            return "FOCUS_PLUS";
        }
        if ("OTHER".equals(normalized)) {
            return "OTHERS";
        }
        return normalized;
    }

    private String formatSchemeType(SchemeApplication.SchemeType schemeType) {
        return switch (schemeType) {
            case CM_CARE -> "CM Care";
            case CM_CONNECT -> "CM Connect";
            case CM_ELEVATE -> "CM Elevate";
            case FOCUS_PLUS -> "Focus+";
            case OTHERS -> "Others";
            default -> schemeType.name();
        };
    }

    private Long visitorIdFromActor(String actor) {
        if (!hasText(actor) || !actor.startsWith("visitor_")) {
            return null;
        }
        try {
            return Long.parseLong(actor.substring("visitor_".length()));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private String trimToNull(String value) {
        return hasText(value) ? value.trim() : null;
    }
}

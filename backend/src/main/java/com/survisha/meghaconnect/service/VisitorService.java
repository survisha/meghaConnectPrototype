package com.survisha.meghaconnect.service;

import com.survisha.meghaconnect.entity.Visitor;
import com.survisha.meghaconnect.repository.VisitorRepository;
import com.survisha.meghaconnect.dto.PublicRegistrationDto;
import com.survisha.meghaconnect.exception.*;
import com.survisha.meghaconnect.util.RequestContextUtil;
import com.survisha.meghaconnect.util.ValidationConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class VisitorService {

    private final VisitorRepository visitorRepository;

    public Optional<Visitor> findByPhone(String phone) {
        return visitorRepository.findByPhoneNumber(phone);
    }

    public Optional<Visitor> findByEpic(String epic) {
        return visitorRepository.findByEpicNumber(epic);
    }

    public Optional<Visitor> findByAadhaar(String aadhaar) {
        return visitorRepository.findByAadhaarNumber(aadhaar);
    }

    public boolean existsByPhone(String phone) {
        return visitorRepository.existsByPhoneNumber(phone);
    }

    public boolean existsByEpicAndPhone(String epic, String phone) {
        return visitorRepository.existsByEpicNumberAndPhoneNumber(epic, phone);
    }

    public List<Visitor> searchByName(String name) {
        return visitorRepository.searchByName(name);
    }

    public List<Visitor> findByConstituency(String constituency) {
        return visitorRepository.findByConstituency(constituency);
    }

    public List<Visitor> findByDistrict(String district) {
        return visitorRepository.findByDistrict(district);
    }

    @Transactional
    public Visitor save(Visitor visitor) {
        return visitorRepository.save(visitor);
    }

    public Optional<Visitor> findById(Long id) {
        return visitorRepository.findById(id);
    }

    @Transactional
    public Visitor registerVisitor(PublicRegistrationDto dto) {
        // Validate required fields
        if (dto.getFullName() == null || dto.getFullName().trim().isEmpty()) {
            throw new VisitorRegistrationValidationException(
                ErrorCodeConstants.FULL_NAME_REQUIRED,
                ErrorCodeConstants.FULL_NAME_REQUIRED_MSG
            );
        }
        if (dto.getPhoneNumber() == null || dto.getPhoneNumber().trim().isEmpty()) {
            throw new VisitorRegistrationValidationException(
                ErrorCodeConstants.PHONE_NUMBER_REQUIRED,
                ErrorCodeConstants.PHONE_NUMBER_REQUIRED_MSG
            );
        }
        if (!dto.getPhoneNumber().matches(ValidationConstants.REGEX_PHONE_NUMBER)) {
            throw new VisitorRegistrationValidationException(
                ErrorCodeConstants.INVALID_PHONE_FORMAT,
                ErrorCodeConstants.INVALID_PHONE_FORMAT_MSG
            );
        }

        // Validate EPIC format if provided
        String normalizedEpic = null;
        if (dto.getEpicNumber() != null && !dto.getEpicNumber().trim().isEmpty()) {
            normalizedEpic = dto.getEpicNumber().trim().toUpperCase();
            if (!normalizedEpic.matches(ValidationConstants.REGEX_EPIC)) {
                throw new VisitorRegistrationValidationException(
                    ErrorCodeConstants.INVALID_EPIC_FORMAT,
                    ErrorCodeConstants.INVALID_EPIC_FORMAT_MSG
                );
            }
        }

        validateKycCompletion(dto);

        String normalizedPhone = dto.getPhoneNumber().trim();
        boolean mobileExists = visitorRepository.existsByPhoneNumber(normalizedPhone);
        if (normalizedEpic != null && visitorRepository.existsByEpicNumberAndPhoneNumber(normalizedEpic, normalizedPhone)) {
            throw new VisitorRegistrationValidationException(
                ErrorCodeConstants.DUPLICATE_EPIC_MOBILE_REGISTRATION,
                ErrorCodeConstants.DUPLICATE_EPIC_MOBILE_REGISTRATION_MSG
            );
        }
        log.info("Visitor final registration validation passed mobileExists={} hasEpic={} hasAadhaar={} phone={}",
                mobileExists,
                normalizedEpic != null,
                dto.getAadhaarNumber() != null && !dto.getAadhaarNumber().trim().isEmpty(),
                RequestContextUtil.maskPhone(normalizedPhone));

        // Validate Aadhaar format if provided
        String maskedAadhaar = null;
        if (dto.getAadhaarNumber() != null && !dto.getAadhaarNumber().trim().isEmpty()) {
            String normalizedAadhaar = dto.getAadhaarNumber().trim();
            if (!normalizedAadhaar.matches(ValidationConstants.REGEX_AADHAAR)) {
                throw new VisitorRegistrationValidationException(
                    ErrorCodeConstants.INVALID_AADHAAR_FORMAT,
                    ErrorCodeConstants.INVALID_AADHAAR_FORMAT_MSG
                );
            }
            maskedAadhaar = maskAadhaar(normalizedAadhaar);
        }

        // Determine KYC type
        String kycType = "NONE";
        if (normalizedEpic != null) {
            kycType = ValidationConstants.ID_TYPE_EPIC;
        } else if (dto.getAadhaarNumber() != null && !dto.getAadhaarNumber().trim().isEmpty()) {
            kycType = ValidationConstants.ID_TYPE_AADHAAR;
        }

        // Determine KYC status
        String kycStatus;
        if (Boolean.TRUE.equals(dto.getManualVerification())) {
            kycStatus = "MANUAL_VERIFICATION_REQUIRED";
        } else if (dto.getKycStatus() != null && !dto.getKycStatus().trim().isEmpty()) {
            kycStatus = dto.getKycStatus().trim();
        } else {
            kycStatus = "PENDING";
        }

        boolean kycVerified = "PHOTO_MATCHED".equals(kycStatus) || "DEMOGRAPHIC_MATCHED".equals(kycStatus);

        // TODO production hardening: validate dto.kycReferenceId against a
        // server-side KYC verification cache/audit record before saving. The
        // current UAT flow only stores the final registration after frontend KYC.

        // Create and save visitor
        Visitor visitor = Visitor.builder()
                .fullName(dto.getFullName().trim())
                .phoneNumber(normalizedPhone)
                .email(dto.getEmail())
                .epicNumber(normalizedEpic)
                .aadhaarNumber(maskedAadhaar)
                .kycType(kycType)
                .kycVerified(kycVerified)
                .kycVerifiedAt(kycVerified ? java.time.LocalDateTime.now() : null)
                .kycStatus(kycStatus)
                .dateOfBirth(parseDate(dto.getDateOfBirth()))
                .gender(trimToNull(dto.getGender()))
                .state(trimToNull(dto.getState()))
                .address(dto.getAddress())
                .designation(dto.getDesignation())
                .district(dto.getDistrict())
                .constituency(dto.getConstituency())
                .booth(dto.getBooth())
                .village(dto.getVillage())
                .photoStoragePath(dto.getPhotoStoragePath())
                .borrowerAddressHouseNumber(trimToNull(dto.getBorrowerAddressHouseNumber()))
                .borrowerAddressSectionNumber(trimToNull(dto.getBorrowerAddressSectionNumber()))
                .relativeNameOnVoterId(trimToNull(dto.getRelativeNameOnVoterId()))
                .pollingPartNo(trimToNull(dto.getPollingPartNo()))
                .pollingStationAddress(trimToNull(dto.getPollingStationAddress()))
                .voterIdVerificationRequestId(trimToNull(dto.getVoterIdVerificationRequestId()))
                .voterIdVerificationCompletionTimestamp(trimToNull(dto.getVoterIdVerificationCompletionTimestamp()))
                .nameMatchScore(dto.getNameMatchScore())
                .idFound(dto.getIdFound())
                .aadhaarClientTxnId(trimToNull(dto.getAadhaarClientTxnId()))
                .aadhaarAppId(trimToNull(dto.getAadhaarAppId()))
                .maskedIdentityNumber(trimToNull(dto.getMaskedIdentityNumber()))
                .build();

        return visitorRepository.save(visitor);
    }

    private void validateKycCompletion(PublicRegistrationDto dto) {
        boolean hasIdentity = (dto.getEpicNumber() != null && !dto.getEpicNumber().trim().isEmpty())
                || (dto.getAadhaarNumber() != null && !dto.getAadhaarNumber().trim().isEmpty());
        if (!hasIdentity) {
            return;
        }

        String status = trimToNull(dto.getKycStatus());
        if (status == null || "PENDING".equalsIgnoreCase(status) || "FAILED".equalsIgnoreCase(status)) {
            throw new VisitorRegistrationValidationException(
                    ErrorCodeConstants.KYC_STATUS_INVALID,
                    "KYC verification must be completed before registration"
            );
        }
    }

    private LocalDate parseDate(String value) {
        String normalized = trimToNull(value);
        if (normalized == null) {
            return null;
        }
        try {
            return LocalDate.parse(normalized);
        } catch (DateTimeParseException e) {
            throw new VisitorRegistrationValidationException(
                    ErrorCodeConstants.INVALID_FIELD_FORMAT,
                    "Date of birth must be in yyyy-MM-dd format"
            );
        }
    }

    private String maskAadhaar(String aadhaarNumber) {
        return "XXXX-XXXX-" + aadhaarNumber.substring(aadhaarNumber.length() - 4);
    }

    private String trimToNull(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return value.trim();
    }
}

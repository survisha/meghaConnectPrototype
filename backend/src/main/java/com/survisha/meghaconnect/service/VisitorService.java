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
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class VisitorService {

    private final VisitorRepository visitorRepository;
    private final FileStorageService fileStorageService;

    public Optional<Visitor> findByPhone(String phone) {
        return visitorRepository.findByPhoneNumber(phone).stream().findFirst();
    }

    public List<Visitor> findAllByPhone(String phone) {
        return visitorRepository.findByPhoneNumber(phone);
    }

    public List<Visitor> findByPhoneAndEpic(String phone, String epic) {
        return visitorRepository.findByPhoneNumberAndEpicNumber(phone, epic);
    }

    public Optional<Visitor> findByEpic(String epic) {
        return visitorRepository.findByEpicNumber(epic);
    }

    public Optional<Visitor> findByAadhaar(String aadhaar) {
        return visitorRepository.findByAadhaarNumber(aadhaar);
    }
    
    public boolean existsByEpicNumber(String epicNumber) {
        return visitorRepository.existsByEpicNumber(epicNumber);
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

    public List<Visitor> search(String mobile, String epic, String referenceId) {
        String normalizedMobile = trimToNull(mobile);
        String normalizedEpic = trimToNull(epic);
        if (normalizedEpic != null) {
            normalizedEpic = normalizedEpic.toUpperCase();
        }
        String normalizedReferenceId = trimToNull(referenceId);

        if (normalizedReferenceId != null) {
            try {
                Long visitorId = Long.parseLong(normalizedReferenceId);
                return visitorRepository.findById(visitorId).map(visitor -> List.of(visitor)).orElse(Collections.emptyList());
            } catch (NumberFormatException ignored) {
                return Collections.emptyList();
            }
        }

        if (normalizedMobile != null && normalizedEpic != null) {
            return visitorRepository.findByPhoneNumberAndEpicNumber(normalizedMobile, normalizedEpic);
        }
        if (normalizedMobile != null) {
            return visitorRepository.findByPhoneNumber(normalizedMobile);
        }
        if (normalizedEpic != null) {
            return visitorRepository.findByEpicNumber(normalizedEpic).map(visitor -> List.of(visitor)).orElse(Collections.emptyList());
        }
        return Collections.emptyList();
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

        String aadhaarClientTxnId = trimToNull(dto.getAadhaarClientTxnId());
        String aadhaarAppId = trimToNull(dto.getAadhaarAppId());
        boolean hasAadhaarKycReference = maskedAadhaar != null || aadhaarClientTxnId != null || aadhaarAppId != null;

        log.info("Visitor final registration validation passed mobileExists={} hasEpic={} hasAadhaarKyc={} phone={}",
                mobileExists,
                normalizedEpic != null,
                hasAadhaarKycReference,
                RequestContextUtil.maskPhone(normalizedPhone));

        // Determine KYC type
        String kycType = "NONE";
        if (normalizedEpic != null) {
            kycType = ValidationConstants.ID_TYPE_EPIC;
        } else if (hasAadhaarKycReference) {
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

        boolean outsideMeghalaya = Boolean.TRUE.equals(dto.getOutsideMeghalaya());
        if (!outsideMeghalaya && trimToNull(dto.getDistrict()) == null) {
            throw new VisitorRegistrationValidationException(
                    ErrorCodeConstants.MISSING_REQUIRED_FIELD,
                    ErrorCodeConstants.format(ErrorCodeConstants.MISSING_REQUIRED_FIELD_MSG, "district")
            );
        }

        String photoPath = resolveLivePhotoPath(dto);
        String fullAddress = firstNonBlank(dto.getFullAddress(), dto.getAddress(), dto.getAddressLine(), dto.getHouseNoColony());
        String addressLine = firstNonBlank(dto.getAddress1(), dto.getAddressLine(), dto.getHouseNoColony(), fullAddress);
        String boothVillage = firstNonBlank(dto.getBoothVillage(), dto.getBooth());
        String location = outsideMeghalaya ? "NA" : trimToNull(dto.getLocation());

        // TODO production hardening: encrypt or tokenize stored file paths before
        // persisting once the production file-system server contract is finalized.
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
                .city(trimToNull(dto.getCity()))
                .pincode(trimToNull(dto.getPincode()))
                .address(fullAddress)
                .fullAddress(fullAddress)
                .address1(addressLine)
                .designation(dto.getDesignation())
                .district(outsideMeghalaya ? firstNonBlank(dto.getDistrict(), "NA") : trimToNull(dto.getDistrict()))
                .constituency(outsideMeghalaya ? firstNonBlank(dto.getConstituency(), "NA") : trimToNull(dto.getConstituency()))
                .booth(outsideMeghalaya ? firstNonBlank(dto.getBooth(), "NA") : trimToNull(dto.getBooth()))
                .boothVillage(outsideMeghalaya ? firstNonBlank(boothVillage, "NA") : boothVillage)
                .village(outsideMeghalaya ? firstNonBlank(dto.getVillage(), "NA") : trimToNull(dto.getVillage()))
                .location(location)
                .outsideMeghalaya(outsideMeghalaya)
                .photoStoragePath(photoPath)
                .livePhotoPath(photoPath)
                .addressLine(addressLine)
                .borrowerAddressHouseNumber(trimToNull(dto.getBorrowerAddressHouseNumber()))
                .borrowerAddressSectionNumber(trimToNull(dto.getBorrowerAddressSectionNumber()))
                .relativeNameOnVoterId(trimToNull(dto.getRelativeNameOnVoterId()))
                .pollingPartNo(trimToNull(dto.getPollingPartNo()))
                .pollingStationAddress(trimToNull(dto.getPollingStationAddress()))
                .voterIdVerificationRequestId(trimToNull(dto.getVoterIdVerificationRequestId()))
                .voterIdVerificationCompletionTimestamp(trimToNull(dto.getVoterIdVerificationCompletionTimestamp()))
                .nameMatchScore(dto.getNameMatchScore())
                .idFound(dto.getIdFound())
                .aadhaarClientTxnId(aadhaarClientTxnId)
                .aadhaarAppId(aadhaarAppId)
                .maskedIdentityNumber(trimToNull(dto.getMaskedIdentityNumber()))
                .build();

        return visitorRepository.save(visitor);
    }

    private void validateKycCompletion(PublicRegistrationDto dto) {
        boolean hasIdentity = (dto.getEpicNumber() != null && !dto.getEpicNumber().trim().isEmpty())
                || (dto.getAadhaarNumber() != null && !dto.getAadhaarNumber().trim().isEmpty())
                || trimToNull(dto.getAadhaarClientTxnId()) != null
                || trimToNull(dto.getAadhaarAppId()) != null;
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

    private String resolveLivePhotoPath(PublicRegistrationDto dto) {
        String existingPath = firstNonBlank(dto.getLivePhotoPath(), dto.getPhotoStoragePath());
        if (existingPath != null) {
            return existingPath;
        }

        if (trimToNull(dto.getLivePhotoBase64()) == null) {
            throw new VisitorRegistrationValidationException(
                    ErrorCodeConstants.INVALID_IMAGE_FORMAT,
                    "Captured image is required."
            );
        }

        try {
            return fileStorageService.storeVisitorPhotoBase64(dto.getLivePhotoBase64());
        } catch (VisitorRegistrationValidationException e) {
            throw e;
        } catch (IOException e) {
            throw new MeghaConnectException(
                    ErrorCodeConstants.FILE_UPLOAD_FAILED,
                    "Visitor live photo storage failed.",
                    500,
                    e
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

    private String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            String normalized = trimToNull(value);
            if (normalized != null) {
                return normalized;
            }
        }
        return null;
    }
}

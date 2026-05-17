package com.survisha.meghaconnect.service;

import com.survisha.meghaconnect.entity.Visitor;
import com.survisha.meghaconnect.repository.VisitorRepository;
import com.survisha.meghaconnect.dto.PublicRegistrationDto;
import com.survisha.meghaconnect.dto.VisitorDto;
import com.survisha.meghaconnect.exception.*;
import com.survisha.meghaconnect.util.DateTimeUtil;
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
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class VisitorService {

    private final VisitorRepository visitorRepository;
    private final FileStorageService fileStorageService;
    private final AuditLogService auditLogService;

    public Optional<Visitor> findByPhone(String phone) {
        return visitorRepository.findByPhoneNumber(phone).stream().findFirst();
    }

    public List<VisitorDto> searchDtos(String mobile, String epic, String referenceId) {
        return toDtos(search(mobile, epic, referenceId));
    }

    public List<VisitorDto> findAllByPhoneDtos(String phone) {
        return toDtos(findAllByPhone(phone));
    }

    public Optional<VisitorDto> findByEpicDto(String epic) {
        return findByEpic(epic).map(this::toDto);
    }

    public List<VisitorDto> searchByNameDtos(String name) {
        return toDtos(searchByName(name));
    }

    public List<VisitorDto> findByDistrictDtos(String district) {
        return toDtos(findByDistrict(district));
    }

    public Optional<VisitorDto> findByIdDto(Long id) {
        return findById(id).map(this::toDto);
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

    @Transactional
    public VisitorDto saveDto(Visitor visitor) {
        return toDto(visitorRepository.save(visitor));
    }

    @Transactional
    public VisitorDto updateVisitor(Long id, VisitorDto dto, String actor) {
        Visitor visitor = visitorRepository.findById(id)
                .orElseThrow(() -> new VisitorNotFoundException(id));

        if (dto == null) {
            return toDto(visitor);
        }

        String normalizedEpic = trimToNull(dto.getEpicNumber());
        if (normalizedEpic != null) {
            normalizedEpic = normalizedEpic.toUpperCase();
            if (!normalizedEpic.matches(ValidationConstants.REGEX_EPIC)) {
                throw new VisitorRegistrationValidationException(
                        ErrorCodeConstants.INVALID_EPIC_FORMAT,
                        ErrorCodeConstants.INVALID_EPIC_FORMAT_MSG
                );
            }
            Optional<Visitor> existingEpicVisitor = visitorRepository.findByEpicNumber(normalizedEpic);
            if (existingEpicVisitor.isPresent() && !existingEpicVisitor.get().getId().equals(id)) {
                throw new VisitorRegistrationValidationException(
                        ErrorCodeConstants.DUPLICATE_EPIC_MOBILE_REGISTRATION,
                        ErrorCodeConstants.DUPLICATE_EPIC_MOBILE_REGISTRATION_MSG
                );
            }
        }

        if (dto.getFullName() != null) visitor.setFullName(firstNonBlank(dto.getFullName(), visitor.getFullName()));
        if (dto.getPhoneNumber() != null) visitor.setPhoneNumber(trimToNull(dto.getPhoneNumber()));
        if (dto.getEpicNumber() != null) visitor.setEpicNumber(normalizedEpic);
        if (dto.getDesignation() != null) visitor.setDesignation(trimToNull(dto.getDesignation()));
        if (dto.getAddress() != null) visitor.setAddress(trimToNull(dto.getAddress()));
        if (dto.getFullAddress() != null) visitor.setFullAddress(trimToNull(dto.getFullAddress()));
        if (dto.getAddress1() != null) visitor.setAddress1(trimToNull(dto.getAddress1()));
        if (dto.getAddressLine() != null) visitor.setAddressLine(trimToNull(dto.getAddressLine()));
        if (dto.getCity() != null) visitor.setCity(trimToNull(dto.getCity()));
        if (dto.getState() != null) visitor.setState(trimToNull(dto.getState()));
        if (dto.getPincode() != null) visitor.setPincode(trimToNull(dto.getPincode()));
        if (dto.getDistrict() != null) visitor.setDistrict(trimToNull(dto.getDistrict()));
        if (dto.getConstituency() != null) visitor.setConstituency(trimToNull(dto.getConstituency()));
        if (dto.getBooth() != null) visitor.setBooth(trimToNull(dto.getBooth()));
        if (dto.getBoothVillage() != null) visitor.setBoothVillage(trimToNull(dto.getBoothVillage()));
        if (dto.getVillage() != null) visitor.setVillage(trimToNull(dto.getVillage()));
        if (dto.getLocation() != null) visitor.setLocation(trimToNull(dto.getLocation()));
        if (dto.getBriefProfile() != null) visitor.setBriefProfile(trimToNull(dto.getBriefProfile()));
        if (dto.getGender() != null) visitor.setGender(trimToNull(dto.getGender()));
        if (dto.getDateOfBirth() != null) visitor.setDateOfBirth(dto.getDateOfBirth());
        if (dto.getOutsideMeghalaya() != null) visitor.setOutsideMeghalaya(dto.getOutsideMeghalaya());

        String livePhotoBase64 = firstNonBlank(dto.getLivePhotoBase64(), dto.getPhotoBase64());
        if (livePhotoBase64 != null) {
            visitor.setLivePhotoPath(storeUpdatedVisitorPhoto(livePhotoBase64));
            visitor.setPhotoStoragePath(visitor.getLivePhotoPath());
        }

        if (visitor.getEpicNumber() != null && !visitor.getEpicNumber().trim().isEmpty()) {
            visitor.setKycType(ValidationConstants.ID_TYPE_EPIC);
        } else if (visitor.getKycType() == null || visitor.getKycType().trim().isEmpty()) {
            visitor.setKycType("NONE");
        }
        if (visitor.getKycVerified() == null) {
            visitor.setKycVerified(false);
        }
        if (visitor.getKycStatus() == null || visitor.getKycStatus().trim().isEmpty()) {
            visitor.setKycStatus("PENDING");
        }
        visitor.setUpdatedBy(firstNonBlank(actor, "visitor-update"));

        return toDto(visitorRepository.save(visitor));
    }

    @Transactional
    public Visitor registerPilotImportedVisitor(String fullName, String phoneNumber,
                                                String addressLocation, String briefProfile,
                                                String actor) {
        String name = firstNonBlank(fullName, "Pilot Visitor");
        String phone = trimToNull(phoneNumber);
        String address = trimToNull(addressLocation);
        String profile = trimToNull(briefProfile);
        String importedBy = firstNonBlank(actor, "pilot-import");

        Visitor visitor = Visitor.builder()
                .fullName(name)
                .phoneNumber(phone)
                .address(address)
                .fullAddress(address)
                .address1(address)
                .addressLine(address)
                .location(address)
                .briefProfile(profile)
                .kycType("NONE")
                .kycVerified(false)
                .kycStatus("PENDING")
                .outsideMeghalaya(false)
                .build();
        visitor.setCreatedBy(importedBy);
        visitor.setUpdatedBy(importedBy);
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

    public VisitorDto toDto(Visitor visitor) {
        if (visitor == null) {
            return null;
        }

        return VisitorDto.builder()
                .id(visitor.getId())
                .fullName(visitor.getFullName())
                .phoneNumber(visitor.getPhoneNumber())
                .epicNumber(visitor.getEpicNumber())
                .aadhaarNumber(maskAadhaarForResponse(visitor.getAadhaarNumber()))
                .kycType(visitor.getKycType())
                .kycProvider(visitor.getKycProvider())
                .kycVerified(visitor.getKycVerified())
                .kycStatus(visitor.getKycStatus())
                .kycFailureReason(visitor.getKycFailureReason())
                .kycRequestId(visitor.getKycRequestId())
                .dateOfBirth(visitor.getDateOfBirth())
                .gender(visitor.getGender())
                .designation(visitor.getDesignation())
                .address(visitor.getAddress())
                .fullAddress(visitor.getFullAddress())
                .address1(visitor.getAddress1())
                .addressLine(visitor.getAddressLine())
                .city(visitor.getCity())
                .state(visitor.getState())
                .pincode(visitor.getPincode())
                .district(visitor.getDistrict())
                .constituency(visitor.getConstituency())
                .booth(visitor.getBooth())
                .boothVillage(visitor.getBoothVillage())
                .village(visitor.getVillage())
                .outsideMeghalaya(visitor.getOutsideMeghalaya())
                .location(visitor.getLocation())
                .briefProfile(visitor.getBriefProfile())
                .photoStoragePath(visitor.getPhotoStoragePath())
                .livePhotoPath(visitor.getLivePhotoPath())
                .photoPath(visitor.getPhotoPath())
                .createdAt(visitor.getCreatedAt())
                .updatedAt(visitor.getUpdatedAt())
                .build();
    }

    public List<VisitorDto> toDtos(List<Visitor> visitors) {
        if (visitors == null || visitors.isEmpty()) {
            return Collections.emptyList();
        }
        return visitors.stream().map(this::toDto).collect(Collectors.toList());
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
            kycStatus = normalizeKycStatus(dto.getKycStatus());
        } else {
            kycStatus = "PENDING";
        }

        boolean kycVerified = "PHOTO_MATCHED".equals(kycStatus) || "DEMOGRAPHIC_MATCHED".equals(kycStatus);
        String kycProvider = firstNonBlank(dto.getKycProvider(), kycType);
        String kycFailureReason = trimToNull(dto.getKycFailureReason());
        String kycRequestId = trimToNull(dto.getKycRequestId());

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
                .kycProvider(kycProvider)
                .kycVerified(kycVerified)
                .kycVerifiedAt(kycVerified ? DateTimeUtil.nowIST() : null)
                .kycFailureReason(kycVerified ? null : kycFailureReason)
                .kycRequestId(kycRequestId)
                .kycLastAttemptAt("KYC_PENDING".equals(kycStatus) ? DateTimeUtil.nowIST() : null)
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

        Visitor saved = visitorRepository.save(visitor);
        if ("KYC_PENDING".equals(kycStatus)) {
            auditLogService.log("Visitor", saved.getId(), "KYC_PENDING_PROCEEDED",
                    "Visitor registered with KYC pending. Provider=" + kycProvider
                            + ", requestId=" + firstNonBlank(kycRequestId, RequestContextUtil.getRequestId()),
                    "visitor_" + saved.getId());
        }
        return saved;
    }

    private void validateKycCompletion(PublicRegistrationDto dto) {
        boolean hasIdentity = (dto.getEpicNumber() != null && !dto.getEpicNumber().trim().isEmpty())
                || (dto.getAadhaarNumber() != null && !dto.getAadhaarNumber().trim().isEmpty())
                || trimToNull(dto.getAadhaarClientTxnId()) != null
                || trimToNull(dto.getAadhaarAppId()) != null;
        if (!hasIdentity) {
            return;
        }

        String status = normalizeKycStatus(dto.getKycStatus());
        if ("KYC_PENDING".equals(status) && Boolean.TRUE.equals(dto.getAllowKycPending())
                && trimToNull(dto.getKycFailureReason()) != null) {
            return;
        }
        if (status == null || "PENDING".equalsIgnoreCase(status) || "KYC_PENDING".equalsIgnoreCase(status)
                || "FAILED".equalsIgnoreCase(status)) {
            throw new VisitorRegistrationValidationException(
                    ErrorCodeConstants.KYC_STATUS_INVALID,
                    "KYC verification must be completed before registration"
            );
        }
    }

    @Transactional
    public Visitor retryKyc(Long visitorId, String actor) {
        Visitor visitor = visitorRepository.findById(visitorId)
                .orElseThrow(() -> new VisitorNotFoundException(visitorId));
        visitor.setKycLastAttemptAt(DateTimeUtil.nowIST());
        visitor.setUpdatedBy(firstNonBlank(actor, "visitor_" + visitorId));
        return visitorRepository.save(visitor);
    }

    @Transactional
    public Visitor markKycVerified(Long visitorId, String status, String requestId, String actor) {
        Visitor visitor = visitorRepository.findById(visitorId)
                .orElseThrow(() -> new VisitorNotFoundException(visitorId));
        visitor.setKycStatus(normalizeKycStatus(firstNonBlank(status, "DEMOGRAPHIC_MATCHED")));
        visitor.setKycVerified(true);
        visitor.setKycVerifiedAt(DateTimeUtil.nowIST());
        visitor.setKycFailureReason(null);
        visitor.setKycRequestId(trimToNull(requestId));
        visitor.setKycLastAttemptAt(DateTimeUtil.nowIST());
        visitor.setUpdatedBy(firstNonBlank(actor, "visitor_" + visitorId));
        Visitor saved = visitorRepository.save(visitor);
        auditLogService.log("Visitor", saved.getId(), "KYC_RETRY_VERIFIED",
                "KYC status changed from pending to verified. requestId=" + firstNonBlank(requestId, RequestContextUtil.getRequestId()),
                firstNonBlank(actor, "visitor_" + visitorId));
        return saved;
    }

    @Transactional
    public Visitor markKycRetryFailed(Long visitorId, String reason, String requestId, String actor) {
        Visitor visitor = visitorRepository.findById(visitorId)
                .orElseThrow(() -> new VisitorNotFoundException(visitorId));
        visitor.setKycStatus("KYC_PENDING");
        visitor.setKycVerified(false);
        visitor.setKycFailureReason(trimToNull(reason));
        visitor.setKycRequestId(trimToNull(requestId));
        visitor.setKycLastAttemptAt(DateTimeUtil.nowIST());
        visitor.setUpdatedBy(firstNonBlank(actor, "visitor_" + visitorId));
        Visitor saved = visitorRepository.save(visitor);
        auditLogService.log("Visitor", saved.getId(), "KYC_RETRY_SERVICE_UNAVAILABLE",
                "KYC retry failed due to provider/service issue. requestId=" + firstNonBlank(requestId, RequestContextUtil.getRequestId()),
                firstNonBlank(actor, "visitor_" + visitorId));
        return saved;
    }

    private String normalizeKycStatus(String status) {
        String value = trimToNull(status);
        if (value == null) {
            return null;
        }
        if ("VERIFIED".equalsIgnoreCase(value)) {
            return "DEMOGRAPHIC_MATCHED";
        }
        if ("MANUAL_REVIEW".equalsIgnoreCase(value)) {
            return "MANUAL_VERIFICATION_REQUIRED";
        }
        return value.trim().toUpperCase();
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

    private String storeUpdatedVisitorPhoto(String livePhotoBase64) {
        try {
            return fileStorageService.storeVisitorPhotoBase64(livePhotoBase64);
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

    private String maskAadhaarForResponse(String aadhaarNumber) {
        String normalized = trimToNull(aadhaarNumber);
        if (normalized == null) {
            return null;
        }
        if (normalized.length() <= 4) {
            return "XXXX-XXXX-" + normalized;
        }
        return "XXXX-XXXX-" + normalized.substring(normalized.length() - 4);
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

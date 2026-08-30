package com.survisha.meghaconnect.service;

import com.survisha.meghaconnect.entity.Visitor;
import com.survisha.meghaconnect.entity.CitizenConsent;
import com.survisha.meghaconnect.entity.MobileOtpVerificationStatus;
import com.survisha.meghaconnect.repository.VisitorRepository;
import com.survisha.meghaconnect.repository.CitizenConsentRepository;
import com.survisha.meghaconnect.dto.AssociateVisitorDto;
import com.survisha.meghaconnect.dto.EpicVerificationData;
import com.survisha.meghaconnect.dto.PublicRegistrationDto;
import com.survisha.meghaconnect.dto.VisitorDto;
import com.survisha.meghaconnect.dto.PollingDetails;
import com.survisha.meghaconnect.face.event.VisitorRegisteredForFaceEnrollmentEvent;
import com.survisha.meghaconnect.exception.*;
import com.survisha.meghaconnect.util.DateTimeUtil;
import com.survisha.meghaconnect.util.RequestContextUtil;
import com.survisha.meghaconnect.util.ValidationConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import com.survisha.meghaconnect.monitoring.MonitoredOperation;
import org.springframework.context.ApplicationEventPublisher;
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

    public static final String REGISTRATION_CONSENT_VERSION = "MC_REG_CONSENT_V1";
    public static final String REGISTRATION_CONSENT_TEXT = "I consent to the capture and use of my photograph for identification and visitor/appointment management purposes. I also consent to the use of my provided voter/EPIC details for searching and verifying my identity for this registration.";

    private final VisitorRepository visitorRepository;
    private final CitizenConsentRepository citizenConsentRepository;
    private final FileStorageService fileStorageService;
    private final AuditLogService auditLogService;
    private final ApplicationEventPublisher eventPublisher;

    @MonitoredOperation(value = "citizen_lookup_by_phone", category = MonitoredOperation.Category.DATABASE)
    public Optional<Visitor> findByPhone(String phone) {
        return visitorRepository.findByPhoneNumber(phone).stream().findFirst();
    }

    @MonitoredOperation(value = "citizen_profile_lookup", category = MonitoredOperation.Category.DATABASE)
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

    @Transactional
    public List<AssociateVisitorDto> searchAssociateCitizens(String query, String actor) {
        String value = trimToNull(query);
        if (value == null || value.length() < 2) {
            return List.of();
        }
        List<AssociateVisitorDto> results = visitorRepository.searchRegisteredCitizens(value).stream()
                .limit(20)
                .map(this::toAssociateVisitorDto)
                .toList();
        auditLogService.log("Visitor", null, "ASSOCIATE_CITIZEN_SEARCH",
                "Associate citizen searched: " + value + ", results=" + results.size(),
                firstNonBlank(actor, "associate-search"));
        return results;
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
        if (dto.getPhoneNumber() != null) {
            String updatedPhone = trimToNull(dto.getPhoneNumber());
            if (!java.util.Objects.equals(visitor.getPhoneNumber(), updatedPhone)) {
                visitor.setMobileOtpVerification(MobileOtpVerificationStatus.NOT_VERIFIED);
            }
            visitor.setPhoneNumber(updatedPhone);
        }
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
        if (dto.getAssemblyConstituencyNumber() != null) visitor.setAssemblyConstituencyNumber(trimToNull(dto.getAssemblyConstituencyNumber()));
        if (dto.getAssemblyConstituencyName() != null) visitor.setAssemblyConstituencyName(trimToNull(dto.getAssemblyConstituencyName()));
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
        String canonicalPhotoPath = firstNonBlank(visitor.getPhotoStoragePath(), visitor.getPhotoPath(), visitor.getLivePhotoPath());
        String livePhotoPath = samePath(visitor.getLivePhotoPath(), canonicalPhotoPath) ? null : trimToNull(visitor.getLivePhotoPath());

        return VisitorDto.builder()
                .id(visitor.getId())
                .fullName(visitor.getFullName())
                .phoneNumber(visitor.getPhoneNumber())
                .mobileOtpVerification(visitor.getMobileOtpVerification())
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
                .assemblyConstituencyNumber(visitor.getAssemblyConstituencyNumber())
                .assemblyConstituencyName(visitor.getAssemblyConstituencyName())
                .booth(visitor.getBooth())
                .boothVillage(visitor.getBoothVillage())
                .village(visitor.getVillage())
                .outsideMeghalaya(visitor.getOutsideMeghalaya())
                .location(visitor.getLocation())
                .briefProfile(visitor.getBriefProfile())
                .agendaType(visitor.getAgendaType())
                .briefDescription(visitor.getBriefDescription())
                .partNumber(visitor.getPollingPartNo())
                .photoStoragePath(canonicalPhotoPath)
                .photoUrl(toUploadUrl(canonicalPhotoPath))
                .livePhotoPath(livePhotoPath)
                .photoPath(visitor.getPhotoPath())
                .createdAt(visitor.getCreatedAt())
                .updatedAt(visitor.getUpdatedAt())
                .build();
    }

    public AssociateVisitorDto toAssociateVisitorDto(Visitor visitor) {
        if (visitor == null) {
            return null;
        }
        return AssociateVisitorDto.builder()
                .citizenId(visitor.getId())
                .fullName(visitor.getFullName())
                .mobileNumber(visitor.getPhoneNumber())
                .epicReference(maskReference(visitor.getEpicNumber()))
                .aadhaarReference(firstNonBlank(visitor.getMaskedIdentityNumber(), maskAadhaarForResponse(visitor.getAadhaarNumber())))
                .addressSummary(firstNonBlank(
                        visitor.getFullAddress(),
                        visitor.getAddress(),
                        visitor.getAddressLine(),
                        visitor.getVillage(),
                        visitor.getLocation()))
                .photoUrl(firstNonBlank(visitor.getLivePhotoPath(), visitor.getPhotoStoragePath(), visitor.getPhotoPath()))
                .kycStatus(firstNonBlank(visitor.getKycStatus(), "PENDING"))
                .status("ACTIVE")
                .role("ASSOCIATE")
                .createdAt(visitor.getCreatedAt())
                .build();
    }

    private String toUploadUrl(String path) {
        String clean = trimToNull(path);
        if (clean == null) {
            return null;
        }
        if (clean.startsWith("http://") || clean.startsWith("https://") || clean.startsWith("/uploads/")) {
            return clean;
        }
        return "/uploads/" + clean.replaceFirst("^/+", "");
    }

    private boolean samePath(String left, String right) {
        String a = trimToNull(left);
        String b = trimToNull(right);
        return a != null && b != null && a.equals(b);
    }

    public List<VisitorDto> toDtos(List<Visitor> visitors) {
        if (visitors == null || visitors.isEmpty()) {
            return Collections.emptyList();
        }
        return visitors.stream().map(this::toDto).collect(Collectors.toList());
    }

    @Transactional
    @MonitoredOperation("visitor_registration")
    public Visitor registerVisitor(PublicRegistrationDto dto) {
        return registerVisitor(dto, MobileOtpVerificationStatus.NOT_VERIFIED);
    }

    @Transactional
    public Visitor registerVisitor(PublicRegistrationDto dto, MobileOtpVerificationStatus mobileOtpStatus) {
        // Validate required fields
        if (dto.getFullName() == null || dto.getFullName().trim().isEmpty()) {
            throw new VisitorRegistrationValidationException(
                ErrorCodeConstants.FULL_NAME_REQUIRED,
                ErrorCodeConstants.FULL_NAME_REQUIRED_MSG
            );
        }
        boolean skipMobileOtp = Boolean.TRUE.equals(dto.getSkipMobileOtpVerification());
        if (!skipMobileOtp && (dto.getPhoneNumber() == null || dto.getPhoneNumber().trim().isEmpty())) {
            throw new VisitorRegistrationValidationException(
                ErrorCodeConstants.PHONE_NUMBER_REQUIRED,
                ErrorCodeConstants.PHONE_NUMBER_REQUIRED_MSG
            );
        }
        if (dto.getPhoneNumber() != null && !dto.getPhoneNumber().trim().isEmpty()
                && !dto.getPhoneNumber().trim().matches(ValidationConstants.REGEX_PHONE_NUMBER)) {
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
        validateConsent(dto);

        String normalizedPhone = trimToNull(dto.getPhoneNumber());
        boolean mobileExists = normalizedPhone != null && visitorRepository.existsByPhoneNumber(normalizedPhone);
        if (normalizedEpic != null && normalizedPhone != null
                && visitorRepository.existsByEpicNumberAndPhoneNumber(normalizedEpic, normalizedPhone)) {
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

        String kycProvider = firstNonBlank(dto.getKycProvider(), kycType);
        // EPIC face claims arriving through a registration payload are display
        // prefill only. Never grant verified status solely from client fields.
        if (kycProvider != null && kycProvider.startsWith("EPIC_FACE")) {
            kycStatus = "MANUAL_VERIFICATION_REQUIRED";
        }
        boolean kycVerified = "PHOTO_MATCHED".equals(kycStatus) || "DEMOGRAPHIC_MATCHED".equals(kycStatus);
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
        String constituency = firstNonBlank(
                dto.getConstituency(),
                formatConstituency(dto.getAssemblyConstituencyName(), dto.getAssemblyConstituencyNumber()));
        String booth = firstNonBlank(dto.getBooth(), dto.getPollingPartNo());

        // TODO production hardening: encrypt or tokenize stored file paths before
        // persisting once the production file-system server contract is finalized.
        // TODO production hardening: validate dto.kycReferenceId against a
        // server-side KYC verification cache/audit record before saving. The
        // current UAT flow only stores the final registration after frontend KYC.

        // Create and save visitor
        Visitor visitor = Visitor.builder()
                .fullName(dto.getFullName().trim())
                .phoneNumber(normalizedPhone)
                .mobileOtpVerification(mobileOtpStatus)
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
                .constituency(outsideMeghalaya ? firstNonBlank(constituency, "NA") : constituency)
                .assemblyConstituencyNumber(trimToNull(dto.getAssemblyConstituencyNumber()))
                .assemblyConstituencyName(trimToNull(dto.getAssemblyConstituencyName()))
                .booth(outsideMeghalaya ? firstNonBlank(booth, "NA") : booth)
                .boothVillage(outsideMeghalaya ? firstNonBlank(boothVillage, "NA") : boothVillage)
                .village(outsideMeghalaya ? firstNonBlank(dto.getVillage(), "NA") : trimToNull(dto.getVillage()))
                .location(location)
                .agendaType(trimToNull(dto.getAgendaType()))
                .briefDescription(trimToNull(dto.getBriefDescription()))
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
                .consentAccepted(dto.getConsentAccepted())
                .consentVersion(trimToNull(dto.getConsentVersion()))
                .consentTimestamp(parseDateTime(dto.getConsentTimestamp()))
                .privacyPolicyUrl(trimToNull(dto.getPrivacyPolicyUrl()))
                .termsUrl(trimToNull(dto.getTermsUrl()))
                .maskedIdentityNumber(trimToNull(dto.getMaskedIdentityNumber()))
                .faceEnrollmentStatus("PENDING")
                .faceEnrollmentMessage("Face enrollment queued after registration.")
                .build();

        Visitor saved = visitorRepository.save(visitor);
        String consentChannel = dto.getConsentChannel().trim().toUpperCase();
        String consentActor = "MOBILE".equals(consentChannel) ? "mobile_registration" : "web_registration";
        citizenConsentRepository.save(CitizenConsent.builder()
                .visitor(saved)
                .consentPurposes("PHOTO_CAPTURE,VOTER_EPIC_SEARCH")
                .consentVersion(REGISTRATION_CONSENT_VERSION)
                .consentText(REGISTRATION_CONSENT_TEXT)
                .consentGranted(true)
                .consentedAt(parseDateTime(dto.getConsentTimestamp()))
                .channel(consentChannel)
                .recordedBy(consentActor)
                .createdAt(DateTimeUtil.nowIST())
                .build());
        auditLogService.log("CITIZEN_REGISTRATION", saved.getId(), "CITIZEN_CONSENT_RECORDED",
                "channel=" + consentChannel + ", purposes=PHOTO_CAPTURE,VOTER_EPIC_SEARCH, version=" + REGISTRATION_CONSENT_VERSION + ", result=GRANTED",
                consentActor);
        eventPublisher.publishEvent(new VisitorRegisteredForFaceEnrollmentEvent(
                saved.getId(), saved.getEpicNumber(), saved.getFullName(), dto.getLivePhotoBase64(),
                dto.getLatitude(), dto.getLongitude()));
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

    private void validateConsent(PublicRegistrationDto dto) {
        if (!Boolean.TRUE.equals(dto.getConsentAccepted())) {
            throw new VisitorRegistrationValidationException(
                    ErrorCodeConstants.MISSING_REQUIRED_FIELD,
                    "Consent is required before collecting identity, photo, document, and appointment data."
            );
        }
        if (!REGISTRATION_CONSENT_VERSION.equals(trimToNull(dto.getConsentVersion()))
                || trimToNull(dto.getConsentTimestamp()) == null
                || !("WEB".equalsIgnoreCase(trimToNull(dto.getConsentChannel()))
                || "MOBILE".equalsIgnoreCase(trimToNull(dto.getConsentChannel())))) {
            throw new VisitorRegistrationValidationException(
                    ErrorCodeConstants.MISSING_REQUIRED_FIELD,
                    "A valid consent version, timestamp, and WEB or MOBILE channel are required."
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
    public Visitor completeEpicKycRetry(Long visitorId, String name, String epicNumber,
                                        EpicVerificationData epicData, String requestId, String actor) {
        Visitor visitor = visitorRepository.findById(visitorId)
                .orElseThrow(() -> new VisitorNotFoundException(visitorId));

        String normalizedEpic = trimToNull(epicNumber);
        if (normalizedEpic != null) {
            normalizedEpic = normalizedEpic.toUpperCase();
        }
        Optional<Visitor> existingEpicVisitor = visitorRepository.findByEpicNumber(normalizedEpic);
        if (existingEpicVisitor.isPresent() && !existingEpicVisitor.get().getId().equals(visitorId)) {
            throw new VisitorRegistrationValidationException(
                    ErrorCodeConstants.DUPLICATE_EPIC_MOBILE_REGISTRATION,
                    ErrorCodeConstants.DUPLICATE_EPIC_MOBILE_REGISTRATION_MSG
            );
        }

        String verifiedName = firstNonBlank(epicData != null ? epicData.getVerifiedName() : null, name);
        String address = buildEpicAddress(epicData);
        PollingDetails pollingDetails = epicData != null ? epicData.getPollingDetails() : null;

        visitor.setFullName(verifiedName);
        visitor.setEpicNumber(normalizedEpic);
        visitor.setKycType(ValidationConstants.ID_TYPE_EPIC);
        visitor.setKycProvider(ValidationConstants.ID_TYPE_EPIC);
        visitor.setKycStatus("KYC_VERIFIED");
        visitor.setKycVerified(true);
        visitor.setKycVerifiedAt(DateTimeUtil.nowIST());
        visitor.setKycFailureReason(null);
        visitor.setKycRequestId(firstNonBlank(requestId, epicData != null ? epicData.getVoterIdVerificationRequestId() : null));
        visitor.setKycLastAttemptAt(DateTimeUtil.nowIST());
        visitor.setUpdatedBy(firstNonBlank(actor, "visitor_" + visitorId));

        if (address != null) {
            visitor.setAddress(address);
            visitor.setFullAddress(address);
            visitor.setAddress1(address);
            visitor.setAddressLine(address);
            visitor.setLocation(address);
        }
        if (epicData != null) {
            visitor.setState(trimToNull(epicData.getBorrowerAddressState()));
            visitor.setDistrict(trimToNull(epicData.getBorrowerAddressDistrict()));
            visitor.setGender(trimToNull(epicData.getBorrowerGender()));
            visitor.setAssemblyConstituencyNumber(trimToNull(epicData.getAssemblyConstituencyNumber()));
            visitor.setAssemblyConstituencyName(trimToNull(epicData.getAssemblyConstituencyName()));
            visitor.setConstituency(firstNonBlank(
                    formatConstituency(epicData.getAssemblyConstituencyName(), epicData.getAssemblyConstituencyNumber()),
                    visitor.getConstituency()));
            visitor.setBorrowerAddressHouseNumber(trimToNull(epicData.getBorrowerAddressHouseNumber()));
            visitor.setBorrowerAddressSectionNumber(trimToNull(epicData.getBorrowerAddressSectionNumber()));
            visitor.setRelativeNameOnVoterId(trimToNull(epicData.getRelativeNameOnVoterId()));
            visitor.setPollingPartNo(trimToNull(pollingDetails != null ? pollingDetails.getPollingPartNo() : null));
            visitor.setBooth(trimToNull(pollingDetails != null ? pollingDetails.getPollingPartNo() : null));
            visitor.setBoothVillage(trimToNull(pollingDetails != null ? pollingDetails.getPollingstationpartname() : null));
            visitor.setNameMatchScore(epicData.getNameMatchScore());
            visitor.setIdFound(epicData.isIdFound());
            visitor.setVoterIdVerificationRequestId(trimToNull(epicData.getVoterIdVerificationRequestId()));
            visitor.setVoterIdVerificationCompletionTimestamp(trimToNull(epicData.getVoterIdVerificationCompletionTimestamp()));
        }

        Visitor saved = visitorRepository.save(visitor);
        auditLogService.log("Visitor", saved.getId(), "KYC_RETRY_VERIFIED",
                "KYC status changed from pending to verified using EPIC. requestId="
                        + firstNonBlank(requestId, RequestContextUtil.getRequestId()),
                firstNonBlank(actor, "visitor_" + visitorId));
        return saved;
    }

    @Transactional
    public Visitor markKycRetryFailed(Long visitorId, String reason, String requestId, String actor) {
        Visitor visitor = visitorRepository.findById(visitorId)
                .orElseThrow(() -> new VisitorNotFoundException(visitorId));
        visitor.setKycStatus("KYC_PENDING");
        visitor.setKycVerified(false);
        visitor.setKycProvider(ValidationConstants.ID_TYPE_EPIC);
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

    private java.time.LocalDateTime parseDateTime(String value) {
        String normalized = trimToNull(value);
        if (normalized == null) {
            return null;
        }
        try {
            return java.time.OffsetDateTime.parse(normalized).toLocalDateTime();
        } catch (DateTimeParseException ignored) {
            try {
                return java.time.LocalDateTime.parse(normalized);
            } catch (DateTimeParseException e) {
                throw new VisitorRegistrationValidationException(
                        ErrorCodeConstants.INVALID_FIELD_FORMAT,
                        "Consent timestamp must be an ISO-8601 date-time"
                );
            }
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

    private String maskReference(String value) {
        String normalized = trimToNull(value);
        if (normalized == null || normalized.length() <= 4) {
            return normalized;
        }
        return "XXXX" + normalized.substring(normalized.length() - 4);
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

    private String formatConstituency(String name, String number) {
        String cleanName = trimToNull(name);
        String cleanNumber = trimToNull(number);
        if (cleanName != null && cleanNumber != null) {
            return cleanName + " / " + cleanNumber;
        }
        return firstNonBlank(cleanName, cleanNumber);
    }

    private String buildEpicAddress(EpicVerificationData data) {
        if (data == null) {
            return null;
        }
        return firstNonBlank(
                joinAddressParts(
                        data.getBorrowerAddressHouseNumber(),
                        data.getBorrowerAddressSectionNumber(),
                        data.getDistrict(),
                        data.getState()),
                data.getDistrict(),
                data.getState());
    }

    private String joinAddressParts(String... parts) {
        if (parts == null) {
            return null;
        }
        StringBuilder builder = new StringBuilder();
        for (String part : parts) {
            String value = trimToNull(part);
            if (value == null || "Not Available".equalsIgnoreCase(value)) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append(", ");
            }
            builder.append(value);
        }
        return trimToNull(builder.toString());
    }
}

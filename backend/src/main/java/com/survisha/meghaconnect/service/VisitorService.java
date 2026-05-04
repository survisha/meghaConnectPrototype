package com.survisha.meghaconnect.service;

import com.survisha.meghaconnect.entity.Visitor;
import com.survisha.meghaconnect.repository.VisitorRepository;
import com.survisha.meghaconnect.dto.PublicRegistrationDto;
import com.survisha.meghaconnect.exception.*;
import com.survisha.meghaconnect.util.ValidationConstants;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
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

        String normalizedPhone = dto.getPhoneNumber().trim();
        if (normalizedEpic != null && visitorRepository.existsByEpicNumberAndPhoneNumber(normalizedEpic, normalizedPhone)) {
            throw new VisitorRegistrationValidationException(
                ErrorCodeConstants.DUPLICATE_EPIC_MOBILE_REGISTRATION,
                ErrorCodeConstants.DUPLICATE_EPIC_MOBILE_REGISTRATION_MSG
            );
        }

        // Validate Aadhaar format if provided
        if (dto.getAadhaarNumber() != null && !dto.getAadhaarNumber().trim().isEmpty()) {
            if (!dto.getAadhaarNumber().matches(ValidationConstants.REGEX_AADHAAR)) {
                throw new VisitorRegistrationValidationException(
                    ErrorCodeConstants.INVALID_AADHAAR_FORMAT,
                    ErrorCodeConstants.INVALID_AADHAAR_FORMAT_MSG
                );
            }
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

        // Create and save visitor
        Visitor visitor = Visitor.builder()
                .fullName(dto.getFullName().trim())
                .phoneNumber(normalizedPhone)
                .email(dto.getEmail())
                .epicNumber(normalizedEpic)
                .aadhaarNumber(dto.getAadhaarNumber())
                .kycType(kycType)
                .kycVerified(kycVerified)
                .kycVerifiedAt(kycVerified ? java.time.LocalDateTime.now() : null)
                .kycStatus(kycStatus)
                .address(dto.getAddress())
                .designation(dto.getDesignation())
                .district(dto.getDistrict())
                .constituency(dto.getConstituency())
                .booth(dto.getBooth())
                .village(dto.getVillage())
                .photoStoragePath(dto.getPhotoStoragePath())
                .build();

        return visitorRepository.save(visitor);
    }
}

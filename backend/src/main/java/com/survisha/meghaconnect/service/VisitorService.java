package com.survisha.meghaconnect.service;

import com.survisha.meghaconnect.entity.Visitor;
import com.survisha.meghaconnect.repository.VisitorRepository;
import com.survisha.meghaconnect.dto.PublicRegistrationDto;
import com.survisha.meghaconnect.exception.*;
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
        if (dto.getPhoneNumber().length() != 10) {
            throw new VisitorRegistrationValidationException(
                ErrorCodeConstants.INVALID_PHONE_FORMAT,
                ErrorCodeConstants.INVALID_PHONE_FORMAT_MSG
            );
        }

        // Check for duplicate mobile
        if (visitorRepository.findByPhoneNumber(dto.getPhoneNumber()).isPresent()) {
            throw new MobileAlreadyRegisteredException(dto.getPhoneNumber());
        }

        // Validate EPIC format if provided
        if (dto.getEpicNumber() != null && !dto.getEpicNumber().trim().isEmpty()) {
            if (!dto.getEpicNumber().matches("^[A-Z]{3}[0-9]{7}$")) {
                throw new VisitorRegistrationValidationException(
                    ErrorCodeConstants.INVALID_EPIC_FORMAT,
                    ErrorCodeConstants.INVALID_EPIC_FORMAT_MSG
                );
            }
        }

        // Validate Aadhaar format if provided
        if (dto.getAadhaarNumber() != null && !dto.getAadhaarNumber().trim().isEmpty()) {
            if (!dto.getAadhaarNumber().matches("^[0-9]{12}$")) {
                throw new VisitorRegistrationValidationException(
                    ErrorCodeConstants.INVALID_AADHAAR_FORMAT,
                    ErrorCodeConstants.INVALID_AADHAAR_FORMAT_MSG
                );
            }
        }

        // Determine KYC type
        String kycType = "NONE";
        if (dto.getEpicNumber() != null && !dto.getEpicNumber().trim().isEmpty()) {
            kycType = "EPIC";
        } else if (dto.getAadhaarNumber() != null && !dto.getAadhaarNumber().trim().isEmpty()) {
            kycType = "AADHAAR";
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
                .phoneNumber(dto.getPhoneNumber().trim())
                .email(dto.getEmail())
                .epicNumber(dto.getEpicNumber())
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

package com.survisha.meghaconnect.service;

import com.survisha.meghaconnect.dto.PublicRegistrationDto;
import com.survisha.meghaconnect.entity.Visitor;
import com.survisha.meghaconnect.exception.ErrorCodeConstants;
import com.survisha.meghaconnect.exception.VisitorNotFoundException;
import com.survisha.meghaconnect.util.ValidationConstants;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class VisitorAuthService {

    private final VisitorOtpService visitorOtpService;
    private final VisitorService visitorService;
    private final RequestValidationService validationService;

    public Map<String, Object> checkMobile(Map<String, String> body) {
        String phone = validationService.requirePhone(body != null ? body.get(ValidationConstants.FIELD_PHONE_NUMBER) : null);
        boolean found = visitorService.findByPhone(phone).isPresent();

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("registered", found);
        response.put("message", found ? "Account found" : "Account not found");
        return response;
    }

    public Map<String, Object> generateOtp(Map<String, String> body) {
        String phone = validationService.requirePhone(body != null ? body.get(ValidationConstants.FIELD_PHONE_NUMBER) : null);
        if (!visitorOtpService.isMobileRegistered(phone)) {
            throw new VisitorNotFoundException(
                    ErrorCodeConstants.format(ErrorCodeConstants.MOBILE_NOT_FOUND_MSG, phone)
            );
        }

        String otp = visitorOtpService.generateOtp(phone);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("otp", otp);
        response.put("message", "OTP sent to " + phone + " (mock)");
        return response;
    }

    public Map<String, Object> validateOtp(Map<String, String> body) {
        String phone = validationService.requirePhone(body != null ? body.get(ValidationConstants.FIELD_PHONE_NUMBER) : null);
        String otp = validationService.requireOtp(body != null ? body.get(ValidationConstants.FIELD_OTP) : null);

        String jwt = visitorOtpService.validateOtpAndLogin(phone, otp);
        Visitor visitor = visitorService.findByPhone(phone)
                .orElseThrow(() -> new VisitorNotFoundException(
                        ErrorCodeConstants.format(ErrorCodeConstants.MOBILE_NOT_FOUND_MSG, phone)
                ));

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("token", jwt);
        response.put("fullName", visitor.getFullName() != null ? visitor.getFullName() : "Visitor");
        response.put("visitorId", visitor.getId() != null ? visitor.getId() : 0L);
        response.put("role", "PUBLIC");
        response.put("message", "Login successful");
        return response;
    }

    public Map<String, Object> register(PublicRegistrationDto dto) {
        Visitor saved = visitorService.registerVisitor(dto);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("visitorId", saved.getId());
        response.put("kycStatus", saved.getKycStatus());
        response.put("kycType", saved.getKycType());
        response.put("message", "Visitor registration completed successfully.");
        return response;
    }

    public Map<String, Object> getProfile(Long visitorId) {
        Visitor visitor = visitorService.findById(visitorId)
                .orElseThrow(() -> new VisitorNotFoundException(visitorId));

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("id", visitor.getId());
        response.put("fullName", visitor.getFullName());
        response.put("phoneNumber", visitor.getPhoneNumber() != null ? visitor.getPhoneNumber() : "");
        response.put("epicNumber", visitor.getEpicNumber() != null ? visitor.getEpicNumber() : "");
        response.put("aadhaarNumber", visitor.getAadhaarNumber() != null ? visitor.getAadhaarNumber() : "");
        response.put("kycType", visitor.getKycType() != null ? visitor.getKycType() : "NONE");
        response.put("kycVerified", Boolean.TRUE.equals(visitor.getKycVerified()));
        response.put("kycStatus", visitor.getKycStatus() != null ? visitor.getKycStatus() : "PENDING");
        response.put("address", visitor.getAddress() != null ? visitor.getAddress() : "");
        response.put("district", visitor.getDistrict() != null ? visitor.getDistrict() : "");
        return response;
    }
}

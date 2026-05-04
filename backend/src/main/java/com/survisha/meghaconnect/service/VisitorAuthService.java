package com.survisha.meghaconnect.service;

import com.survisha.meghaconnect.dto.PublicRegistrationDto;
import com.survisha.meghaconnect.entity.Visitor;
import com.survisha.meghaconnect.exception.ErrorCodeConstants;
import com.survisha.meghaconnect.exception.VisitorNotFoundException;
import com.survisha.meghaconnect.util.ValidationConstants;
import com.survisha.meghaconnect.util.RequestContextUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class VisitorAuthService {

    private final VisitorOtpService visitorOtpService;
    private final VisitorService visitorService;
    private final RequestValidationService validationService;

    public Map<String, Object> checkMobile(Map<String, String> body) {
        String phone = validationService.requirePhone(body != null ? body.get(ValidationConstants.FIELD_PHONE_NUMBER) : null);
        boolean found = visitorService.existsByPhone(phone);
        log.info("Visitor mobile availability checked mobileExists={} phone={}",
                found, RequestContextUtil.maskPhone(phone));

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("registered", found);
        response.put("message", found ? "Account found" : "Account not found");
        return response;
    }

    public Map<String, Object> checkRegistration(Map<String, String> body) {
        String phone = validationService.requirePhone(body != null ? body.get(ValidationConstants.FIELD_PHONE_NUMBER) : null);
        String epic = null;
        if (body != null && !validationService.isBlank(body.get(ValidationConstants.FIELD_EPIC_NUMBER))) {
            epic = validationService.requireEpic(body.get(ValidationConstants.FIELD_EPIC_NUMBER));
        }

        boolean mobileExists = visitorService.existsByPhone(phone);
        boolean epicMobileExists = epic != null && visitorService.existsByEpicAndPhone(epic, phone);

        log.info("Visitor registration duplicate check mobileExists={} epicMobileExists={} phone={}",
                mobileExists, epicMobileExists, RequestContextUtil.maskPhone(phone));

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("mobileExists", mobileExists);
        response.put("epicMobileExists", epicMobileExists);

        if (epicMobileExists) {
            response.put("message", ErrorCodeConstants.DUPLICATE_EPIC_MOBILE_REGISTRATION_MSG);
        } else if (mobileExists) {
            response.put("message", ErrorCodeConstants.MOBILE_ALREADY_REGISTERED_WARNING_MSG);
        } else {
            response.put("message", "Mobile number is available for registration.");
        }
        return response;
    }

    public Map<String, Object> generateOtp(Map<String, String> body) {
        String phone = validationService.requirePhone(body != null ? body.get(ValidationConstants.FIELD_PHONE_NUMBER) : null);
        boolean registrationFlow = isRegistrationOtpRequest(body);
        String otp;
        if (registrationFlow) {
            otp = visitorOtpService.generateKycOtp(phone);
        } else {
            if (!visitorOtpService.isMobileRegistered(phone)) {
                throw new VisitorNotFoundException(
                        ErrorCodeConstants.format(ErrorCodeConstants.MOBILE_NOT_FOUND_MSG, phone)
                );
            }
            otp = visitorOtpService.generateOtp(phone);
        }
        log.info("Visitor OTP generated purpose={} phone={}",
                registrationFlow ? "REGISTRATION" : "LOGIN",
                RequestContextUtil.maskPhone(phone));

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("otp", otp);
        response.put("message", "OTP sent to " + phone + " (mock)");
        return response;
    }

    private boolean isRegistrationOtpRequest(Map<String, String> body) {
        if (body == null) {
            return false;
        }
        String purpose = body.get("purpose");
        String registrationFlow = body.get("registrationFlow");
        return "REGISTRATION".equalsIgnoreCase(purpose)
                || "true".equalsIgnoreCase(registrationFlow);
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

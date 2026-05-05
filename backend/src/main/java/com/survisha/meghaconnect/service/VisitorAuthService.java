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
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class VisitorAuthService {

    private static final String CODE_OTP_SENT = "OTP_SENT";
    private static final String CODE_MULTIPLE_REGISTRATIONS_FOUND = "MULTIPLE_REGISTRATIONS_FOUND";
    private static final String CODE_VISITOR_NOT_FOUND = "VISITOR_NOT_FOUND";
    private static final String CODE_MOBILE_EPIC_NOT_FOUND = "MOBILE_EPIC_NOT_FOUND";
    private static final String CODE_DUPLICATE_REGISTRATION_FOUND = "DUPLICATE_REGISTRATION_FOUND";

    private static final String MSG_MULTIPLE_REGISTRATIONS_FOUND =
            "Multiple registrations are linked with this mobile number. Please enter your EPIC number to continue.";
    private static final String MSG_VISITOR_NOT_FOUND =
            "No registration found for this mobile number.";
    private static final String MSG_MOBILE_EPIC_NOT_FOUND =
            "No registration found for the entered mobile number and EPIC number.";
    private static final String MSG_DUPLICATE_REGISTRATION_FOUND =
            "Duplicate registration found. Please contact support.";
    private static final String MSG_OTP_SENT = "OTP sent successfully.";

    private final VisitorOtpService visitorOtpService;
    private final VisitorService visitorService;
    private final RequestValidationService validationService;

    public Map<String, Object> checkMobile(Map<String, String> body) {
        String phone = validationService.requirePhone(body != null ? body.get(ValidationConstants.FIELD_PHONE_NUMBER) : null);
        List<Visitor> visitors = visitorService.findAllByPhone(phone);
        boolean found = !visitors.isEmpty();
        log.info("Visitor mobile availability checked mobileExists={} registrationCount={} phone={}",
                found, visitors.size(), RequestContextUtil.maskPhone(phone));

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("registered", found);
        response.put("registrationCount", visitors.size());
        response.put("requiresEpic", visitors.size() > 1);
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
        boolean epicExists = epic != null && visitorService.existsByEpicNumber(epic);

        log.info("Visitor registration duplicate check mobileExists={} epicMobileExists={} phone={}",
                mobileExists, epicMobileExists, RequestContextUtil.maskPhone(phone));

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("mobileExists", mobileExists);
        response.put("epicMobileExists", epicMobileExists);
        response.put("epicExists", epicExists);

        if (epicMobileExists || epicExists) {
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
            LoginResolution resolution = resolveLoginVisitor(phone, optionalEpic(body));
            if (!resolution.success) {
                return loginResolutionResponse(resolution);
            }
            otp = visitorOtpService.generateOtp(phone, resolution.visitor.getId());
        }
        log.info("Visitor OTP generated purpose={} phone={}",
                registrationFlow ? "REGISTRATION" : "LOGIN",
                RequestContextUtil.maskPhone(phone));

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("code", CODE_OTP_SENT);
        response.put("requiresEpic", false);
        response.put("otp", otp);
        response.put("message", MSG_OTP_SENT);
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
        LoginResolution resolution = resolveLoginVisitor(phone, optionalEpic(body));
        if (!resolution.success) {
            return loginResolutionResponse(resolution);
        }

        String jwt = visitorOtpService.validateOtpAndLogin(phone, otp, resolution.visitor.getId());
        Visitor visitor = resolution.visitor;

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("code", "LOGIN_SUCCESS");
        response.put("requiresEpic", false);
        response.put("token", jwt);
        response.put("fullName", visitor.getFullName() != null ? visitor.getFullName() : "Visitor");
        response.put("visitorId", visitor.getId() != null ? visitor.getId() : 0L);
        response.put("role", "PUBLIC");
        response.put("message", "Login successful");
        return response;
    }

    private String optionalEpic(Map<String, String> body) {
        if (body == null || validationService.isBlank(body.get(ValidationConstants.FIELD_EPIC_NUMBER))) {
            return null;
        }
        return validationService.requireEpic(body.get(ValidationConstants.FIELD_EPIC_NUMBER));
    }

    private LoginResolution resolveLoginVisitor(String phone, String epic) {
        if (epic == null) {
            List<Visitor> visitors = visitorService.findAllByPhone(phone);
            if (visitors.isEmpty()) {
                log.info("Visitor login resolution no mobile match phone={}", RequestContextUtil.maskPhone(phone));
                return LoginResolution.failure(CODE_VISITOR_NOT_FOUND, MSG_VISITOR_NOT_FOUND, false);
            }
            if (visitors.size() > 1) {
                log.info("Visitor login resolution requires EPIC phone={} registrationCount={}",
                        RequestContextUtil.maskPhone(phone), visitors.size());
                return LoginResolution.failure(CODE_MULTIPLE_REGISTRATIONS_FOUND, MSG_MULTIPLE_REGISTRATIONS_FOUND, true);
            }
            return LoginResolution.success(visitors.get(0));
        }

        List<Visitor> matches = visitorService.findByPhoneAndEpic(phone, epic);
        if (matches.isEmpty()) {
            log.info("Visitor login resolution no phone+EPIC match phone={}", RequestContextUtil.maskPhone(phone));
            return LoginResolution.failure(CODE_MOBILE_EPIC_NOT_FOUND, MSG_MOBILE_EPIC_NOT_FOUND, true);
        }
        if (matches.size() > 1) {
            log.warn("Visitor login resolution duplicate phone+EPIC match phone={} count={}",
                    RequestContextUtil.maskPhone(phone), matches.size());
            return LoginResolution.failure(CODE_DUPLICATE_REGISTRATION_FOUND, MSG_DUPLICATE_REGISTRATION_FOUND, true);
        }
        return LoginResolution.success(matches.get(0));
    }

    private Map<String, Object> loginResolutionResponse(LoginResolution resolution) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("code", resolution.code);
        response.put("message", resolution.message);
        response.put("requiresEpic", resolution.requiresEpic);
        return response;
    }

    private static class LoginResolution {
        private final boolean success;
        private final Visitor visitor;
        private final String code;
        private final String message;
        private final boolean requiresEpic;

        private LoginResolution(boolean success, Visitor visitor, String code, String message, boolean requiresEpic) {
            this.success = success;
            this.visitor = visitor;
            this.code = code;
            this.message = message;
            this.requiresEpic = requiresEpic;
        }

        private static LoginResolution success(Visitor visitor) {
            return new LoginResolution(true, visitor, CODE_OTP_SENT, MSG_OTP_SENT, false);
        }

        private static LoginResolution failure(String code, String message, boolean requiresEpic) {
            return new LoginResolution(false, null, code, message, requiresEpic);
        }
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
        response.put("addressLine", visitor.getAddressLine() != null ? visitor.getAddressLine() : "");
        response.put("district", visitor.getDistrict() != null ? visitor.getDistrict() : "");
        response.put("constituency", visitor.getConstituency() != null ? visitor.getConstituency() : "");
        response.put("boothVillage", visitor.getBoothVillage() != null ? visitor.getBoothVillage() : "");
        response.put("outsideMeghalaya", Boolean.TRUE.equals(visitor.getOutsideMeghalaya()));
        response.put("location", visitor.getLocation() != null ? visitor.getLocation() : "");
        response.put("livePhotoPath", visitor.getLivePhotoPath() != null ? visitor.getLivePhotoPath() : "");
        return response;
    }
}

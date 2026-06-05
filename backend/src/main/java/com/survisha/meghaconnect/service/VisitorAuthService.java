package com.survisha.meghaconnect.service;

import com.survisha.common.sms.SmsService;
import com.survisha.meghaconnect.dto.EpicVerificationRequest;
import com.survisha.meghaconnect.dto.EpicVerificationResponse;
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
            "Multiple registrations are linked with this mobile number. Please select your registration to continue.";
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
    private final FileStorageService fileStorageService;
    private final EpicVerificationService epicVerificationService;
    private final AuditLogService auditLogService;
    private final SmsService smsService;

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

        if (epicExists) {
            response.put("message", ErrorCodeConstants.DUPLICATE_EPIC_REGISTRATION_MSG);
        } else if (epicMobileExists) {
            response.put("message", ErrorCodeConstants.DUPLICATE_EPIC_MOBILE_REGISTRATION_MSG);
        } else if (mobileExists) {
            response.put("message", ErrorCodeConstants.MOBILE_ALREADY_REGISTERED_WARNING_MSG);
        } else {
            response.put("message", "Mobile number is available for registration.");
        }
        return response;
    }

    public Map<String, Object> searchRegistrations(Map<String, String> body) {
        String phone = validationService.requirePhone(body != null ? body.get(ValidationConstants.FIELD_PHONE_NUMBER) : null);
        List<Visitor> visitors = visitorService.findAllByPhone(phone);
        log.info("Visitor login registrations searched registrationCount={} phone={}",
                visitors.size(), RequestContextUtil.maskPhone(phone));

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("registered", !visitors.isEmpty());
        response.put("registrationCount", visitors.size());
        response.put("requiresEpic", visitors.size() > 1);
        response.put("registrations", visitors.stream().map(this::loginRegistrationOption).toList());
        response.put("message", visitors.isEmpty() ? MSG_VISITOR_NOT_FOUND : "Registrations found.");
        return response;
    }

    public Map<String, Object> generateOtp(Map<String, String> body) {
        String phone = validationService.requirePhone(body != null ? body.get(ValidationConstants.FIELD_PHONE_NUMBER) : null);
        boolean registrationFlow = isRegistrationOtpRequest(body);
        if (registrationFlow) {
            visitorOtpService.generateKycOtp(phone);
        } else {
            LoginResolution resolution = resolveLoginVisitor(phone, optionalEpic(body), optionalVisitorId(body));
            if (!resolution.success) {
                return loginResolutionResponse(resolution);
            }
            visitorOtpService.generateOtp(phone, resolution.visitor.getId());
        }
        log.info("Visitor OTP generated purpose={} phone={}",
                registrationFlow ? "REGISTRATION" : "LOGIN",
                RequestContextUtil.maskPhone(phone));

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("code", CODE_OTP_SENT);
        response.put("requiresEpic", false);
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

        if (isRegistrationOtpRequest(body)) {
            boolean valid = visitorOtpService.validateKycOtp(phone, otp);
            Map<String, Object> response = new HashMap<>();
            response.put("success", valid);
            response.put("code", valid ? "OTP_VALIDATED" : "OTP_INVALID");
            response.put("requiresEpic", false);
            response.put("message", valid ? "OTP validated successfully" : "Invalid OTP. Please try again.");
            return response;
        }

        LoginResolution resolution = resolveLoginVisitor(phone, optionalEpic(body), optionalVisitorId(body));
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
        response.put("kycStatus", visitor.getKycStatus() != null ? visitor.getKycStatus() : "PENDING");
        response.put("kycPending", "KYC_PENDING".equalsIgnoreCase(visitor.getKycStatus()));
        response.put("message", "OTP validated successfully");
        return response;
    }

    private String optionalEpic(Map<String, String> body) {
        if (body == null || validationService.isBlank(body.get(ValidationConstants.FIELD_EPIC_NUMBER))) {
            return null;
        }
        return validationService.requireEpic(body.get(ValidationConstants.FIELD_EPIC_NUMBER));
    }

    private Long optionalVisitorId(Map<String, String> body) {
        if (body == null || validationService.isBlank(body.get("visitorId"))) {
            return null;
        }
        try {
            Long visitorId = Long.parseLong(body.get("visitorId").trim());
            return visitorId > 0 ? visitorId : null;
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private LoginResolution resolveLoginVisitor(String phone, String epic, Long visitorId) {
        if (visitorId != null) {
            return visitorService.findById(visitorId)
                    .map(visitor -> {
                        if (visitor.getPhoneNumber() == null || !visitor.getPhoneNumber().equals(phone)) {
                            log.info("Visitor login resolution selected visitor phone mismatch visitorId={} phone={}",
                                    visitorId, RequestContextUtil.maskPhone(phone));
                            return LoginResolution.failure(CODE_VISITOR_NOT_FOUND, MSG_VISITOR_NOT_FOUND, false);
                        }
                        if (epic != null && (visitor.getEpicNumber() == null
                                || !visitor.getEpicNumber().equalsIgnoreCase(epic))) {
                            log.info("Visitor login resolution selected visitor EPIC mismatch visitorId={} phone={}",
                                    visitorId, RequestContextUtil.maskPhone(phone));
                            return LoginResolution.failure(CODE_MOBILE_EPIC_NOT_FOUND, MSG_MOBILE_EPIC_NOT_FOUND, true);
                        }
                        return LoginResolution.success(visitor);
                    })
                    .orElseGet(() -> LoginResolution.failure(CODE_VISITOR_NOT_FOUND, MSG_VISITOR_NOT_FOUND, false));
        }

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

    private Map<String, Object> loginRegistrationOption(Visitor visitor) {
        Map<String, Object> option = new HashMap<>();
        option.put("visitorId", visitor.getId());
        option.put("fullName", firstNonBlank(visitor.getFullName(), "Visitor"));
        option.put("epicNumber", firstNonBlank(visitor.getEpicNumber()));
        option.put("maskedEpicNumber", maskReference(visitor.getEpicNumber()));
        option.put("kycStatus", firstNonBlank(visitor.getKycStatus(), "PENDING"));
        option.put("district", firstNonBlank(visitor.getDistrict()));
        option.put("constituency", firstNonBlank(visitor.getAssemblyConstituencyName(), visitor.getConstituency()));
        return option;
    }

    private String maskReference(String value) {
        String clean = firstNonBlank(value);
        if (clean.length() <= 4) {
            return clean;
        }
        return "****" + clean.substring(clean.length() - 4);
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
        smsService.sendRegistrationSuccessSms(saved.getPhoneNumber(), registrationReference(saved));

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("visitorId", saved.getId());
        response.put("kycStatus", saved.getKycStatus());
        response.put("kycType", saved.getKycType());
        response.put("kycProvider", saved.getKycProvider());
        response.put("requestId", RequestContextUtil.getRequestId());
        response.put("canProceed", true);
        response.put("message", "KYC_PENDING".equalsIgnoreCase(saved.getKycStatus())
                ? "Registration completed with KYC pending. Please retry verification later."
                : "Visitor registration completed successfully.");
        return response;
    }

    private String registrationReference(Visitor visitor) {
        return visitor != null && visitor.getId() != null ? "VIS" + visitor.getId() : "N/A";
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
        response.put("kycProvider", visitor.getKycProvider() != null ? visitor.getKycProvider() : visitor.getKycType());
        response.put("kycVerified", Boolean.TRUE.equals(visitor.getKycVerified()));
        response.put("kycStatus", visitor.getKycStatus() != null ? visitor.getKycStatus() : "PENDING");
        response.put("kycFailureReason", visitor.getKycFailureReason() != null ? visitor.getKycFailureReason() : "");
        response.put("kycRequestId", visitor.getKycRequestId() != null ? visitor.getKycRequestId() : "");
        response.put("designation", visitor.getDesignation() != null ? visitor.getDesignation() : "");
        response.put("address", visitor.getAddress() != null ? visitor.getAddress() : "");
        response.put("fullAddress", visitor.getFullAddress() != null ? visitor.getFullAddress() : "");
        response.put("address1", visitor.getAddress1() != null ? visitor.getAddress1() : "");
        response.put("addressLine", visitor.getAddressLine() != null ? visitor.getAddressLine() : "");
        response.put("city", visitor.getCity() != null ? visitor.getCity() : "");
        response.put("state", visitor.getState() != null ? visitor.getState() : "");
        response.put("pincode", visitor.getPincode() != null ? visitor.getPincode() : "");
        response.put("district", visitor.getDistrict() != null ? visitor.getDistrict() : "");
        String constituency = firstNonBlank(
                formatConstituency(visitor.getAssemblyConstituencyName(), visitor.getAssemblyConstituencyNumber()),
                visitor.getConstituency());
        String boothVillage = firstNonBlank(visitor.getPollingPartNo(), visitor.getBoothVillage(), visitor.getBooth());
        response.put("constituency", constituency);
        response.put("assemblyConstituencyName", visitor.getAssemblyConstituencyName() != null ? visitor.getAssemblyConstituencyName() : "");
        response.put("assemblyConstituencyNumber", visitor.getAssemblyConstituencyNumber() != null ? visitor.getAssemblyConstituencyNumber() : "");
        response.put("boothVillage", boothVillage);
        response.put("booth", visitor.getBooth() != null ? visitor.getBooth() : "");
        response.put("partNumber", visitor.getPollingPartNo() != null ? visitor.getPollingPartNo() : "");
        response.put("pollingPartNo", visitor.getPollingPartNo() != null ? visitor.getPollingPartNo() : "");
        response.put("agendaType", visitor.getAgendaType() != null ? visitor.getAgendaType() : "");
        response.put("briefDescription", visitor.getBriefDescription() != null ? visitor.getBriefDescription() : "");
        response.put("outsideMeghalaya", Boolean.TRUE.equals(visitor.getOutsideMeghalaya()));
        response.put("location", visitor.getLocation() != null ? visitor.getLocation() : "");
        String visitorPhotoPath = firstNonBlank(visitor.getLivePhotoPath(), visitor.getPhotoStoragePath(), visitor.getPhotoPath());
        response.put("livePhotoPath", visitorPhotoPath);
        response.put("photoStoragePath", visitor.getPhotoStoragePath() != null ? visitor.getPhotoStoragePath() : "");
        response.put("photoPath", visitor.getPhotoPath() != null ? visitor.getPhotoPath() : "");
        String livePhotoBase64 = fileStorageService.loadImageDataUri(visitorPhotoPath).orElse("");
        response.put("livePhotoBase64", livePhotoBase64);
        response.put("photoBase64", livePhotoBase64);
        response.put("photoUrl", livePhotoBase64);
        return response;
    }

    public Map<String, Object> retryKyc(Long visitorId) {
        Visitor visitor = visitorService.retryKyc(visitorId, "visitor_" + visitorId);
        auditLogService.log("Visitor", visitorId, "KYC_RETRY_ATTEMPTED",
                "KYC retry attempted. Provider=" + firstNonBlank(visitor.getKycProvider(), visitor.getKycType()),
                "visitor_" + visitorId);

        if (!ValidationConstants.ID_TYPE_EPIC.equalsIgnoreCase(visitor.getKycType())) {
            Visitor updated = visitorService.markKycRetryFailed(
                    visitorId,
                    "Aadhaar retry requires a fresh QR verification from the registration screen.",
                    RequestContextUtil.getRequestId(),
                    "visitor_" + visitorId);
            return retryResponse(updated, false,
                    "KYC service is still unavailable. Please try after some time.");
        }

        EpicVerificationResponse epicResponse = epicVerificationService.verifyEpic(EpicVerificationRequest.builder()
                .epicNumber(visitor.getEpicNumber())
                .visitorName(visitor.getFullName())
                .phoneNumber(visitor.getPhoneNumber())
                .build());

        if (epicResponse != null && epicResponse.isSuccess()) {
            Visitor updated = visitorService.markKycVerified(
                    visitorId,
                    "DEMOGRAPHIC_MATCHED",
                    epicResponse.getRequestId(),
                    "visitor_" + visitorId);
            return retryResponse(updated, true, "KYC verification completed successfully.");
        }

        if (epicResponse != null && isServiceUnavailable(epicResponse.getCode(), epicResponse.getMessage())) {
            Visitor updated = visitorService.markKycRetryFailed(
                    visitorId,
                    epicResponse.getMessage(),
                    epicResponse.getRequestId(),
                    "visitor_" + visitorId);
            return retryResponse(updated, false,
                    "KYC service is still unavailable. Please try after some time.");
        }

        Map<String, Object> response = retryResponse(visitor, false,
                epicResponse != null ? epicResponse.getMessage() : "KYC verification failed.");
        response.put("hardFailure", true);
        return response;
    }

    private Map<String, Object> retryResponse(Visitor visitor, boolean verified, String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", verified);
        response.put("visitorId", visitor.getId());
        response.put("kycStatus", visitor.getKycStatus());
        response.put("kycProvider", firstNonBlank(visitor.getKycProvider(), visitor.getKycType()));
        response.put("requestId", RequestContextUtil.getRequestId());
        response.put("message", message);
        response.put("canProceed", true);
        return response;
    }

    private boolean isServiceUnavailable(String code, String message) {
        String text = (code + " " + message).toLowerCase();
        return "503".equals(code)
                || text.contains("unavailable")
                || text.contains("timeout")
                || text.contains("gateway")
                || text.contains("provider")
                || text.contains("ovse")
                || text.contains("client error");
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return "";
        }
        for (String value : values) {
            if (value != null && !value.trim().isEmpty()) {
                return value.trim();
            }
        }
        return "";
    }

    private String formatConstituency(String name, String number) {
        String cleanName = firstNonBlank(name);
        String cleanNumber = firstNonBlank(number);
        if (!cleanName.isEmpty() && !cleanNumber.isEmpty()) {
            return cleanName + " / " + cleanNumber;
        }
        return firstNonBlank(cleanName, cleanNumber);
    }
}

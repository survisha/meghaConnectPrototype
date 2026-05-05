package com.survisha.meghaconnect.service;

import com.survisha.meghaconnect.exception.ErrorCodeConstants;
import com.survisha.meghaconnect.exception.RequestValidationException;
import com.survisha.meghaconnect.util.ValidationConstants;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class VisitorKycService {

    private final VisitorOtpService visitorOtpService;
    private final RequestValidationService validationService;

    /**
     * Backward-compatible mock ID validation. Real EPIC/Aadhaar provider calls
     * live in KycController and its services.
     */
    public Map<String, Object> validateIdType(Map<String, String> request) {
        String idType = validationService.requireKycIdType(value(request, ValidationConstants.FIELD_ID_TYPE));
        String idNumber = identityValue(request);
        validationService.validateKycIdentity(idType, idNumber);

        String providedPhone = validationService.optionalPhone(value(request, ValidationConstants.FIELD_PHONE_NUMBER));
        boolean manualVerification = providedPhone != null;
        String otpTargetPhone = manualVerification ? providedPhone : ValidationConstants.MOCK_KYC_PHONE_NUMBER;

        String otp = visitorOtpService.generateKycOtp(otpTargetPhone);
        String maskedPhone = maskPhone(otpTargetPhone);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("otpSent", true);
        response.put("otp", otp);
        response.put("phoneNumber", maskedPhone);
        response.put("actualPhoneNumber", otpTargetPhone);
        response.put("manualVerification", manualVerification);
        response.put("message", "OTP sent to " + maskedPhone);
        return response;
    }

    public Map<String, Object> verifyOtp(Map<String, String> request) {
        String otp = validationService.requireOtp(value(request, ValidationConstants.FIELD_OTP));
        String idNumber = validationService.requireText(identityValue(request), ValidationConstants.FIELD_ID_NUMBER);
        String phoneNumber = validationService.requirePhone(value(request, ValidationConstants.FIELD_PHONE_NUMBER));
        String idType = validationService.requireKycIdType(value(request, ValidationConstants.FIELD_ID_TYPE));

        boolean isValid = visitorOtpService.validateKycOtp(phoneNumber, otp);
        if (!isValid) {
            throw new RequestValidationException(
                    ErrorCodeConstants.OTP_INVALID,
                    "Invalid OTP. Please try again.",
                    401
            );
        }

        Map<String, String> demographics = buildMockDemographics(idType, idNumber);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("verified", true);
        response.put("demographics", demographics);
        response.put("message", "OTP verified successfully");
        return response;
    }

    public Map<String, Object> validateFace(Map<String, String> request) {
        validationService.requireText(identityValue(request), ValidationConstants.FIELD_ID_NUMBER);
        validationService.requireImageDataUri(value(request, ValidationConstants.FIELD_LIVE_PHOTO));

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("matched", true);
        response.put("kycStatus", "PHOTO_MATCHED");
        response.put("confidence", 95.5);
        response.put("message", "Face matched successfully");
        return response;
    }

    private Map<String, String> buildMockDemographics(String idType, String idNumber) {
        Map<String, String> demographics = new HashMap<>();
        if (ValidationConstants.ID_TYPE_EPIC.equals(idType)) {
            demographics.put("fullName", "Rajesh Kumar Sharma");
            demographics.put("address", "Laitumkhrah, Shillong");
            demographics.put("district", "East Khasi Hills");
            demographics.put("constituency", "Shillong North");
        } else if (ValidationConstants.ID_TYPE_AADHAAR.equals(idType)) {
            demographics.put("fullName", "Priya Singh");
            demographics.put("address", "Police Bazar, Shillong");
            demographics.put("district", "East Khasi Hills");
            demographics.put("constituency", "Shillong Central");
        } else {
            demographics.put("fullName", "Unknown");
            demographics.put("address", "");
            demographics.put("district", "");
            demographics.put("constituency", "");
        }
        demographics.put("idNumber", idNumber);
        return demographics;
    }

    private String maskPhone(String phone) {
        return "XXXX-XXXX-" + phone.substring(phone.length() - 4);
    }

    private String value(Map<String, String> request, String key) {
        return request != null ? request.get(key) : null;
    }

    private String identityValue(Map<String, String> request) {
        String idNumber = value(request, ValidationConstants.FIELD_ID_NUMBER);
        return validationService.isBlank(idNumber) ? value(request, ValidationConstants.FIELD_ID_VALUE) : idNumber;
    }
}

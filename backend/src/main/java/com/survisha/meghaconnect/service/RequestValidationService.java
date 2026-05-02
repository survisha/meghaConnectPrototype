package com.survisha.meghaconnect.service;

import com.survisha.meghaconnect.exception.ErrorCodeConstants;
import com.survisha.meghaconnect.exception.RequestValidationException;
import com.survisha.meghaconnect.util.ValidationConstants;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.Map;

@Service
public class RequestValidationService {

    public String requireText(Map<String, ?> body, String fieldName) {
        Object value = body != null ? body.get(fieldName) : null;
        return requireText(value != null ? value.toString() : null, fieldName);
    }

    public String requireText(String value, String fieldName) {
        if (isBlank(value)) {
            throw new RequestValidationException(
                    ErrorCodeConstants.MISSING_REQUIRED_FIELD,
                    ErrorCodeConstants.format(ErrorCodeConstants.MISSING_REQUIRED_FIELD_MSG, fieldName)
            );
        }
        return value.trim();
    }

    public String requirePhone(String phoneNumber) {
        String value = requireText(phoneNumber, ValidationConstants.FIELD_PHONE_NUMBER);
        if (!value.matches(ValidationConstants.REGEX_PHONE_NUMBER)) {
            throw new RequestValidationException(
                    ErrorCodeConstants.INVALID_PHONE_FORMAT,
                    ErrorCodeConstants.INVALID_PHONE_FORMAT_MSG
            );
        }
        return value;
    }

    public String optionalPhone(String phoneNumber) {
        if (isBlank(phoneNumber)) {
            return null;
        }
        return requirePhone(phoneNumber);
    }

    public String requireOtp(String otp) {
        String value = requireText(otp, ValidationConstants.FIELD_OTP);
        if (!value.matches(ValidationConstants.REGEX_OTP)) {
            throw new RequestValidationException(
                    ErrorCodeConstants.INVALID_OTP_LENGTH,
                    ErrorCodeConstants.INVALID_OTP_LENGTH_MSG
            );
        }
        return value;
    }

    public String requireEpic(String epicNumber) {
        String value = requireText(epicNumber, ValidationConstants.FIELD_ID_NUMBER).toUpperCase();
        if (!value.matches(ValidationConstants.REGEX_EPIC)) {
            throw new RequestValidationException(
                    ErrorCodeConstants.INVALID_EPIC_FORMAT,
                    ErrorCodeConstants.INVALID_EPIC_FORMAT_MSG
            );
        }
        return value;
    }

    public String requireAadhaar(String aadhaarNumber) {
        String value = requireText(aadhaarNumber, ValidationConstants.FIELD_ID_NUMBER);
        if (!value.matches(ValidationConstants.REGEX_AADHAAR)) {
            throw new RequestValidationException(
                    ErrorCodeConstants.INVALID_AADHAAR_FORMAT,
                    ErrorCodeConstants.INVALID_AADHAAR_FORMAT_MSG
            );
        }
        return value;
    }

    public String requireKycIdType(String idType) {
        String value = requireText(idType, ValidationConstants.FIELD_ID_TYPE).toUpperCase();
        if (!ValidationConstants.ID_TYPE_EPIC.equals(value)
                && !ValidationConstants.ID_TYPE_AADHAAR.equals(value)) {
            throw new RequestValidationException(
                    ErrorCodeConstants.INVALID_IDENTITY_DOCUMENT,
                    ErrorCodeConstants.INVALID_IDENTITY_DOCUMENT_MSG
            );
        }
        return value;
    }

    public void validateKycIdentity(String idType, String idNumber) {
        String normalizedIdType = requireKycIdType(idType);
        if (ValidationConstants.ID_TYPE_EPIC.equals(normalizedIdType)) {
            requireEpic(idNumber);
        } else {
            requireAadhaar(idNumber);
        }
    }

    public void requireImageDataUri(String imageDataUri) {
        String value = requireText(imageDataUri, ValidationConstants.FIELD_LIVE_PHOTO);
        if (!value.startsWith("data:image/")) {
            throw new RequestValidationException(
                    ErrorCodeConstants.INVALID_IMAGE_FORMAT,
                    ValidationConstants.MSG_VALID_BASE64_IMAGE_REQUIRED
            );
        }
    }

    public <E extends Enum<E>> E requireEnum(String value, Class<E> enumType, String fieldName) {
        String raw = requireText(value, fieldName);
        try {
            return Enum.valueOf(enumType, raw.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new RequestValidationException(
                    ErrorCodeConstants.INVALID_FIELD_VALUE,
                    String.format(ValidationConstants.MSG_INVALID_ENUM, fieldName, value)
            );
        }
    }

    public LocalDateTime requireDateTime(Object value, String fieldName) {
        if (value == null || isBlank(value.toString())) {
            throw new RequestValidationException(
                    ErrorCodeConstants.MISSING_REQUIRED_FIELD,
                    ErrorCodeConstants.format(ErrorCodeConstants.MISSING_REQUIRED_FIELD_MSG, fieldName)
            );
        }
        try {
            return LocalDateTime.parse(value.toString());
        } catch (DateTimeParseException e) {
            throw new RequestValidationException(
                    ErrorCodeConstants.INVALID_DATE_TIME_FORMAT,
                    String.format(ValidationConstants.MSG_INVALID_DATETIME, fieldName, value)
            );
        }
    }

    public int requireInteger(Object value, String fieldName) {
        if (value == null || isBlank(value.toString())) {
            throw new RequestValidationException(
                    ErrorCodeConstants.MISSING_REQUIRED_FIELD,
                    ErrorCodeConstants.format(ErrorCodeConstants.MISSING_REQUIRED_FIELD_MSG, fieldName)
            );
        }
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException e) {
            throw new RequestValidationException(
                    ErrorCodeConstants.INVALID_FIELD_FORMAT,
                    String.format(ValidationConstants.MSG_INTEGER_REQUIRED, fieldName)
            );
        }
    }

    public Long optionalLong(Object value, String fieldName) {
        if (value == null || isBlank(value.toString())) {
            return null;
        }
        try {
            return Long.parseLong(value.toString());
        } catch (NumberFormatException e) {
            throw new RequestValidationException(
                    ErrorCodeConstants.INVALID_FIELD_FORMAT,
                    String.format(ValidationConstants.MSG_INTEGER_REQUIRED, fieldName)
            );
        }
    }

    public void requireNonEmptyFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new RequestValidationException(
                    ErrorCodeConstants.FILE_UPLOAD_FAILED,
                    "File is empty"
            );
        }
    }

    public boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}

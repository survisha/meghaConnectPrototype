package com.survisha.meghaconnect.util;

public final class ValidationConstants {

    private ValidationConstants() {}

    public static final String REGEX_PHONE_NUMBER = "^[0-9]{10}$";
    public static final String REGEX_OTP = "^[0-9]{6}$";
    public static final String REGEX_EPIC = "^[A-Z]{3}[0-9]{7}$";
    public static final String REGEX_AADHAAR = "^[0-9]{12}$";

    public static final String ID_TYPE_EPIC = "EPIC";
    public static final String ID_TYPE_AADHAAR = "AADHAAR";
    public static final String ID_TYPE_NONE = "NONE";

    public static final String FIELD_PHONE_NUMBER = "phoneNumber";
    public static final String FIELD_OTP = "otp";
    public static final String FIELD_ID_TYPE = "idType";
    public static final String FIELD_ID_NUMBER = "idNumber";
    public static final String FIELD_ID_VALUE = "idValue";
    public static final String FIELD_EPIC_NUMBER = "epicNumber";
    public static final String FIELD_VISITOR_NAME = "visitorName";
    public static final String FIELD_AADHAAR = "aadhaar";
    public static final String FIELD_LIVE_PHOTO = "livePhoto";
    public static final String FIELD_STATUS = "status";
    public static final String FIELD_SCHEDULED_DATE_TIME = "scheduledDateTime";
    public static final String FIELD_DURATION_MINUTES = "durationMinutes";
    public static final String FIELD_CLIENT_TXN_ID = "clientTxnId";

    public static final String MOCK_KYC_PHONE_NUMBER = "9876543210";

    public static final String MSG_REQUIRED_FIELD = "%s is required";
    public static final String MSG_REQUIRED_FIELDS = "%s are required";
    public static final String MSG_INVALID_ENUM = "Invalid %s: %s";
    public static final String MSG_INVALID_DATETIME = "Invalid date-time format for %s: %s";
    public static final String MSG_INTEGER_REQUIRED = "%s must be an integer";
    public static final String MSG_VALID_BASE64_IMAGE_REQUIRED = "Live photo must be a valid base64 encoded image";
}

package com.survisha.meghaconnect.exception;

/**
 * Centralized error code constants for MeghaConnect application.
 * All error codes and their corresponding messages are defined here.
 * This ensures consistency across the entire application.
 */
public class ErrorCodeConstants {

    // ========== GENERAL ERRORS ==========
    public static final String GENERAL_ERROR = "ERR_000";
    public static final String GENERAL_ERROR_MSG = "An unexpected error occurred";

    // ========== INPUT VALIDATION ERRORS (ERR_001 - ERR_009) ==========
    public static final String FULL_NAME_REQUIRED = "ERR_001";
    public static final String FULL_NAME_REQUIRED_MSG = "Full name is required";

    public static final String PHONE_NUMBER_REQUIRED = "ERR_002";
    public static final String PHONE_NUMBER_REQUIRED_MSG = "Phone number is required";

    public static final String INVALID_PHONE_FORMAT = "ERR_003";
    public static final String INVALID_PHONE_FORMAT_MSG = "Phone number must be exactly 10 digits";

    public static final String INVALID_EPIC_FORMAT = "ERR_004";
    public static final String INVALID_EPIC_FORMAT_MSG = "EPIC number must be 3 uppercase letters followed by 7 digits (e.g., ABC1234567)";

    public static final String INVALID_AADHAAR_FORMAT = "ERR_005";
    public static final String INVALID_AADHAAR_FORMAT_MSG = "Aadhaar number must be exactly 12 digits";

    public static final String INVALID_EMAIL_FORMAT = "ERR_006";
    public static final String INVALID_EMAIL_FORMAT_MSG = "Invalid email format";

    public static final String DESIGNATION_REQUIRED = "ERR_007";
    public static final String DESIGNATION_REQUIRED_MSG = "Designation is required";

    public static final String INVALID_IDENTITY_DOCUMENT = "ERR_008";
    public static final String INVALID_IDENTITY_DOCUMENT_MSG = "Invalid identity document format";

    public static final String MISSING_REQUIRED_FIELD = "ERR_009";
    public static final String MISSING_REQUIRED_FIELD_MSG = "Required field is missing: %s";

    // ========== RESOURCE NOT FOUND ERRORS (ERR_010 - ERR_019) ==========
    public static final String VISITOR_NOT_FOUND = "ERR_010";
    public static final String VISITOR_NOT_FOUND_MSG = "Visitor not found with id: %s";

    public static final String APPOINTMENT_NOT_FOUND = "ERR_011";
    public static final String APPOINTMENT_NOT_FOUND_MSG = "Appointment not found with id: %s";

    public static final String USER_NOT_FOUND = "ERR_012";
    public static final String USER_NOT_FOUND_MSG = "User not found with id: %s";

    public static final String MOBILE_NOT_FOUND = "ERR_013";
    public static final String MOBILE_NOT_FOUND_MSG = "No registered user found with mobile number: %s";

    public static final String SCHEDULE_EVENT_NOT_FOUND = "ERR_014";
    public static final String SCHEDULE_EVENT_NOT_FOUND_MSG = "Schedule event not found with id: %s";

    public static final String GRIEVANCE_NOT_FOUND = "ERR_015";
    public static final String GRIEVANCE_NOT_FOUND_MSG = "Grievance not found with id: %s";

    public static final String DIRECTION_NOT_FOUND = "ERR_016";
    public static final String DIRECTION_NOT_FOUND_MSG = "Direction not found with id: %s";

    // ========== BUSINESS LOGIC ERRORS (ERR_020 - ERR_029) ==========
    public static final String MOBILE_ALREADY_REGISTERED = "ERR_020";
    public static final String MOBILE_ALREADY_REGISTERED_MSG = "Mobile number is already registered. Please use a different mobile or login.";

    public static final String SCHEDULING_CONFLICT = "ERR_021";
    public static final String SCHEDULING_CONFLICT_MSG = "Scheduling conflict detected. A schedule event already exists at: %s";

    public static final String INVALID_APPOINTMENT_STATUS = "ERR_022";
    public static final String INVALID_APPOINTMENT_STATUS_MSG = "Invalid appointment status: %s";

    public static final String INVALID_STATUS_TRANSITION = "ERR_023";
    public static final String INVALID_STATUS_TRANSITION_MSG = "Cannot transition appointment from %s to %s";

    public static final String FILE_UPLOAD_FAILED = "ERR_024";
    public static final String FILE_UPLOAD_FAILED_MSG = "File upload failed: %s";

    public static final String FILE_SIZE_EXCEEDED = "ERR_025";
    public static final String FILE_SIZE_EXCEEDED_MSG = "File size exceeds maximum allowed limit of %s MB";

    public static final String INVALID_FILE_TYPE = "ERR_026";
    public static final String INVALID_FILE_TYPE_MSG = "Invalid file type. Allowed types: %s";

    public static final String APPOINTMENT_ALREADY_SCHEDULED = "ERR_027";
    public static final String APPOINTMENT_ALREADY_SCHEDULED_MSG = "Appointment is already scheduled and cannot be modified";

    public static final String INVALID_MEETING_LOCATION = "ERR_028";
    public static final String INVALID_MEETING_LOCATION_MSG = "Invalid meeting location: %s";

    public static final String DUPLICATE_ENTRY = "ERR_029";
    public static final String DUPLICATE_ENTRY_MSG = "Duplicate entry found: %s";

    // ========== OTP & AUTHENTICATION ERRORS (ERR_030 - ERR_039) ==========
    public static final String OTP_RATE_LIMIT_EXCEEDED = "ERR_030";
    public static final String OTP_RATE_LIMIT_EXCEEDED_MSG = "Too many OTP requests. Please try again after %d minutes";

    public static final String OTP_EXPIRED_OR_NOT_FOUND = "ERR_031";
    public static final String OTP_EXPIRED_OR_NOT_FOUND_MSG = "OTP has expired or was not found. Please request a new OTP";

    public static final String OTP_MAX_ATTEMPTS_EXCEEDED = "ERR_032";
    public static final String OTP_MAX_ATTEMPTS_EXCEEDED_MSG = "Maximum OTP verification attempts exceeded. Please request a new OTP";

    public static final String OTP_INVALID = "ERR_033";
    public static final String OTP_INVALID_MSG = "Invalid OTP. %d attempts remaining";

    public static final String INVALID_CREDENTIALS = "ERR_034";
    public static final String INVALID_CREDENTIALS_MSG = "Invalid username or password";

    public static final String USER_NOT_AUTHENTICATED = "ERR_035";
    public static final String USER_NOT_AUTHENTICATED_MSG = "User authentication failed";

    public static final String TOKEN_EXPIRED = "ERR_036";
    public static final String TOKEN_EXPIRED_MSG = "JWT token has expired";

    public static final String TOKEN_INVALID = "ERR_037";
    public static final String TOKEN_INVALID_MSG = "Invalid JWT token";

    public static final String UNAUTHORIZED_ACCESS = "ERR_038";
    public static final String UNAUTHORIZED_ACCESS_MSG = "You do not have permission to access this resource";

    public static final String INVALID_OTP_LENGTH = "ERR_039";
    public static final String INVALID_OTP_LENGTH_MSG = "OTP must be exactly 6 digits";

    // ========== EXTERNAL SERVICE ERRORS (ERR_040 - ERR_049) ==========
    public static final String EXTERNAL_SERVICE_ERROR = "ERR_040";
    public static final String EXTERNAL_SERVICE_ERROR_MSG = "External service error: %s";

    public static final String EPIC_VERIFICATION_FAILED = "ERR_041";
    public static final String EPIC_VERIFICATION_FAILED_MSG = "EPIC verification failed: %s";

    public static final String AADHAAR_VERIFICATION_FAILED = "ERR_042";
    public static final String AADHAAR_VERIFICATION_FAILED_MSG = "Aadhaar verification failed: %s";

    public static final String KYC_VERIFICATION_FAILED = "ERR_043";
    public static final String KYC_VERIFICATION_FAILED_MSG = "KYC verification failed: %s";

    public static final String AI_SERVICE_ERROR = "ERR_044";
    public static final String AI_SERVICE_ERROR_MSG = "AI service error: %s";

    public static final String OVSE_SERVICE_ERROR = "ERR_045";
    public static final String OVSE_SERVICE_ERROR_MSG = "OVSE service error: %s";

    public static final String QR_GENERATION_FAILED = "ERR_046";
    public static final String QR_GENERATION_FAILED_MSG = "QR code generation failed: %s";

    public static final String EPIC_NAME_MISMATCH = "ERR_047";
    public static final String EPIC_NAME_MISMATCH_MSG = "Name mismatch from Voter ID.";

    // ========== DATABASE & DATA ACCESS ERRORS (ERR_050 - ERR_059) ==========
    public static final String DATABASE_ERROR = "ERR_050";
    public static final String DATABASE_ERROR_MSG = "Database error occurred";

    public static final String TRANSACTION_FAILED = "ERR_051";
    public static final String TRANSACTION_FAILED_MSG = "Transaction failed: %s";

    public static final String DATA_INTEGRITY_ERROR = "ERR_052";
    public static final String DATA_INTEGRITY_ERROR_MSG = "Data integrity constraint violation: %s";

    // ========== CONFIGURATION & SYSTEM ERRORS (ERR_060 - ERR_069) ==========
    public static final String CONFIGURATION_ERROR = "ERR_060";
    public static final String CONFIGURATION_ERROR_MSG = "Configuration error: %s";

    public static final String INVALID_CONFIGURATION = "ERR_061";
    public static final String INVALID_CONFIGURATION_MSG = "Invalid application configuration";

    public static final String MISSING_CONFIGURATION = "ERR_062";
    public static final String MISSING_CONFIGURATION_MSG = "Missing required configuration: %s";

    // ========== ROLE & PERMISSION ERRORS (ERR_070 - ERR_079) ==========
    public static final String INSUFFICIENT_PERMISSIONS = "ERR_070";
    public static final String INSUFFICIENT_PERMISSIONS_MSG = "Insufficient permissions for this action";

    public static final String INVALID_ROLE = "ERR_071";
    public static final String INVALID_ROLE_MSG = "Invalid user role: %s";

    public static final String ROLE_NOT_FOUND = "ERR_072";
    public static final String ROLE_NOT_FOUND_MSG = "Role not found: %s";

    // ========== VISITOR REGISTRATION ERRORS (ERR_080 - ERR_089) ==========
    public static final String VISITOR_REGISTRATION_VALIDATION_ERROR = "ERR_080";
    public static final String VISITOR_REGISTRATION_VALIDATION_ERROR_MSG = "Visitor registration validation failed: %s";

    public static final String VISITOR_ALREADY_EXISTS = "ERR_081";
    public static final String VISITOR_ALREADY_EXISTS_MSG = "Visitor with this mobile number already exists";

    public static final String INVALID_VISITOR_DATA = "ERR_082";
    public static final String INVALID_VISITOR_DATA_MSG = "Invalid visitor data: %s";

    public static final String KYC_STATUS_INVALID = "ERR_083";
    public static final String KYC_STATUS_INVALID_MSG = "Invalid KYC status: %s";

    public static final String INVALID_FIELD_VALUE = "ERR_084";
    public static final String INVALID_FIELD_VALUE_MSG = "Invalid field value: %s";

    public static final String INVALID_FIELD_FORMAT = "ERR_085";
    public static final String INVALID_FIELD_FORMAT_MSG = "Invalid field format: %s";

    public static final String INVALID_DATE_TIME_FORMAT = "ERR_086";
    public static final String INVALID_DATE_TIME_FORMAT_MSG = "Invalid date-time format: %s";

    public static final String INVALID_IMAGE_FORMAT = "ERR_087";
    public static final String INVALID_IMAGE_FORMAT_MSG = "Invalid image format";

    // ========== STREAM & CONTENT ERRORS (ERR_090 - ERR_099) ==========
    public static final String CONTENT_NOT_FOUND = "ERR_090";
    public static final String CONTENT_NOT_FOUND_MSG = "Content not found: %s";

    public static final String INVALID_CONTENT_TYPE = "ERR_091";
    public static final String INVALID_CONTENT_TYPE_MSG = "Invalid content type: %s";

    // Utility method to format message with arguments
    public static String format(String message, Object... args) {
        try {
            return String.format(message, args);
        } catch (Exception e) {
            return message;
        }
    }
}

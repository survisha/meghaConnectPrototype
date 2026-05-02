package com.survisha.meghaconnect.exception;

/**
 * Exception thrown when visitor registration validation fails.
 * Includes specific validation errors for required fields, format, and business rules.
 */
public class VisitorRegistrationValidationException extends MeghaConnectException {
    
    public VisitorRegistrationValidationException(String message) {
        super(ErrorCodeConstants.VISITOR_REGISTRATION_VALIDATION_ERROR, message, 400);
    }

    public VisitorRegistrationValidationException(String errorCode, String message) {
        super(errorCode, message, 400);
    }

    public VisitorRegistrationValidationException(String message, Throwable cause) {
        super(ErrorCodeConstants.VISITOR_REGISTRATION_VALIDATION_ERROR, message, 400, cause);
    }
}

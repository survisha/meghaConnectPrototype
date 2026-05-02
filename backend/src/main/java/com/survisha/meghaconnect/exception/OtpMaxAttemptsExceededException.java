package com.survisha.meghaconnect.exception;

/**
 * Exception thrown when maximum OTP verification attempts have been exceeded.
 * User has tried to verify OTP too many times with incorrect values.
 */
public class OtpMaxAttemptsExceededException extends MeghaConnectException {
    
    public OtpMaxAttemptsExceededException() {
        super(ErrorCodeConstants.OTP_MAX_ATTEMPTS_EXCEEDED, 
              ErrorCodeConstants.OTP_MAX_ATTEMPTS_EXCEEDED_MSG, 
              429);
    }

    public OtpMaxAttemptsExceededException(String message) {
        super(ErrorCodeConstants.OTP_MAX_ATTEMPTS_EXCEEDED, message, 429);
    }

    public OtpMaxAttemptsExceededException(String message, Throwable cause) {
        super(ErrorCodeConstants.OTP_MAX_ATTEMPTS_EXCEEDED, message, 429, cause);
    }
}

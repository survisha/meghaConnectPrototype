package com.survisha.meghaconnect.exception;

/**
 * Exception thrown when OTP has expired or was not found.
 * Occurs when attempting to verify an expired or non-existent OTP.
 */
public class OtpExpiredException extends MeghaConnectException {
    
    public OtpExpiredException() {
        super(ErrorCodeConstants.OTP_EXPIRED_OR_NOT_FOUND, 
              ErrorCodeConstants.OTP_EXPIRED_OR_NOT_FOUND_MSG, 
              401);
    }

    public OtpExpiredException(String message) {
        super(ErrorCodeConstants.OTP_EXPIRED_OR_NOT_FOUND, message, 401);
    }

    public OtpExpiredException(String message, Throwable cause) {
        super(ErrorCodeConstants.OTP_EXPIRED_OR_NOT_FOUND, message, 401, cause);
    }
}

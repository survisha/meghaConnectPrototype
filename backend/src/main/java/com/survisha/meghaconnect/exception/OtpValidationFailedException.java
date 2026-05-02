package com.survisha.meghaconnect.exception;

/**
 * Exception thrown when OTP validation fails (invalid OTP entered).
 * Includes information about remaining attempts.
 */
public class OtpValidationFailedException extends MeghaConnectException {
    
    private final int remainingAttempts;

    public OtpValidationFailedException(int remainingAttempts) {
        super(ErrorCodeConstants.OTP_INVALID, 
              String.format(ErrorCodeConstants.OTP_INVALID_MSG, remainingAttempts), 
              401);
        this.remainingAttempts = remainingAttempts;
    }

    public OtpValidationFailedException(String message, int remainingAttempts) {
        super(ErrorCodeConstants.OTP_INVALID, message, 401);
        this.remainingAttempts = remainingAttempts;
    }

    public OtpValidationFailedException(String message, int remainingAttempts, Throwable cause) {
        super(ErrorCodeConstants.OTP_INVALID, message, 401, cause);
        this.remainingAttempts = remainingAttempts;
    }

    public int getRemainingAttempts() {
        return remainingAttempts;
    }
}

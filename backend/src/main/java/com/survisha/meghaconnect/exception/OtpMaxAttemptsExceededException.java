package com.survisha.meghaconnect.exception;

/**
 * Exception thrown when maximum OTP verification attempts have been exceeded.
 * User has tried to verify OTP too many times with incorrect values.
 */
public class OtpMaxAttemptsExceededException extends MeghaConnectException {
    private static final int WAIT_TIME_MINUTES = 30;
    
    public OtpMaxAttemptsExceededException() {
        super(ErrorCodeConstants.OTP_LOCKED, 
              ErrorCodeConstants.OTP_LOCKED_MSG, 
              429);
    }

    public OtpMaxAttemptsExceededException(String message) {
        super(ErrorCodeConstants.OTP_LOCKED, message, 429);
    }

    public OtpMaxAttemptsExceededException(String message, Throwable cause) {
        super(ErrorCodeConstants.OTP_LOCKED, message, 429, cause);
    }

    public int getWaitTimeMinutes() {
        return WAIT_TIME_MINUTES;
    }
}

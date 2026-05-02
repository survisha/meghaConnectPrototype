package com.survisha.meghaconnect.exception;

/**
 * Exception thrown when OTP request rate limit is exceeded.
 * User has sent too many OTP requests in a short time period.
 */
public class OtpRateLimitExceededException extends MeghaConnectException {
    
    private final int waitTimeMinutes;

    public OtpRateLimitExceededException(int waitTimeMinutes) {
        super(ErrorCodeConstants.OTP_RATE_LIMIT_EXCEEDED, 
              String.format(ErrorCodeConstants.OTP_RATE_LIMIT_EXCEEDED_MSG, waitTimeMinutes), 
              429);
        this.waitTimeMinutes = waitTimeMinutes;
    }

    public OtpRateLimitExceededException(String message, int waitTimeMinutes) {
        super(ErrorCodeConstants.OTP_RATE_LIMIT_EXCEEDED, message, 429);
        this.waitTimeMinutes = waitTimeMinutes;
    }

    public int getWaitTimeMinutes() {
        return waitTimeMinutes;
    }
}

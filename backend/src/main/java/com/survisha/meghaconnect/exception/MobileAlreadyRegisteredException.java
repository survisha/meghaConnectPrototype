package com.survisha.meghaconnect.exception;

/**
 * Exception thrown when a mobile number is already registered.
 * Prevents duplicate registrations for the same phone number.
 */
public class MobileAlreadyRegisteredException extends MeghaConnectException {
    
    public MobileAlreadyRegisteredException(String mobileNumber) {
        super(ErrorCodeConstants.MOBILE_ALREADY_REGISTERED, 
              ErrorCodeConstants.MOBILE_ALREADY_REGISTERED_MSG, 
              409);
    }

    public MobileAlreadyRegisteredException(String message, Throwable cause) {
        super(ErrorCodeConstants.MOBILE_ALREADY_REGISTERED, message, 409, cause);
    }
}

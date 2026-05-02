package com.survisha.meghaconnect.exception;

/**
 * Exception thrown when an appointment is not found.
 * Replaces generic ResourceNotFoundException for appointment-specific errors.
 */
public class AppointmentNotFoundException extends MeghaConnectException {
    
    public AppointmentNotFoundException(Long appointmentId) {
        super(ErrorCodeConstants.APPOINTMENT_NOT_FOUND, 
              String.format(ErrorCodeConstants.APPOINTMENT_NOT_FOUND_MSG, appointmentId), 
              404);
    }

    public AppointmentNotFoundException(String message) {
        super(ErrorCodeConstants.APPOINTMENT_NOT_FOUND, message, 404);
    }

    public AppointmentNotFoundException(String message, Throwable cause) {
        super(ErrorCodeConstants.APPOINTMENT_NOT_FOUND, message, 404, cause);
    }
}

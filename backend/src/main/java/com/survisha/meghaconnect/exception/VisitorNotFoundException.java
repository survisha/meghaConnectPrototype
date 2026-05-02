package com.survisha.meghaconnect.exception;

/**
 * Exception thrown when a visitor is not found.
 * Replaces generic ResourceNotFoundException for visitor-specific errors.
 */
public class VisitorNotFoundException extends MeghaConnectException {
    
    public VisitorNotFoundException(Long visitorId) {
        super(ErrorCodeConstants.VISITOR_NOT_FOUND, 
              String.format(ErrorCodeConstants.VISITOR_NOT_FOUND_MSG, visitorId), 
              404);
    }

    public VisitorNotFoundException(String message) {
        super(ErrorCodeConstants.VISITOR_NOT_FOUND, message, 404);
    }

    public VisitorNotFoundException(String message, Throwable cause) {
        super(ErrorCodeConstants.VISITOR_NOT_FOUND, message, 404, cause);
    }
}

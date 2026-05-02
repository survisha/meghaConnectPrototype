package com.survisha.meghaconnect.exception;

/**
 * Exception thrown when external service calls fail.
 * Covers EPIC verification, Aadhaar verification, OVSE, AI services, etc.
 */
public class ExternalServiceException extends MeghaConnectException {
    
    private final String serviceName;

    public ExternalServiceException(String serviceName, String message) {
        super(ErrorCodeConstants.EXTERNAL_SERVICE_ERROR, message, 503);
        this.serviceName = serviceName;
    }

    public ExternalServiceException(String serviceName, String message, Throwable cause) {
        super(ErrorCodeConstants.EXTERNAL_SERVICE_ERROR, message, 503, cause);
        this.serviceName = serviceName;
    }

    public ExternalServiceException(String errorCode, String serviceName, String message, int httpStatus) {
        super(errorCode, message, httpStatus);
        this.serviceName = serviceName;
    }

    public ExternalServiceException(String errorCode, String serviceName, String message, int httpStatus, Throwable cause) {
        super(errorCode, message, httpStatus, cause);
        this.serviceName = serviceName;
    }

    public String getServiceName() {
        return serviceName;
    }
}

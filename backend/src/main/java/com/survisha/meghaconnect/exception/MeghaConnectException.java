package com.survisha.meghaconnect.exception;

/**
 * Base custom exception for MeghaConnect application.
 * All business logic exceptions should extend this class.
 * Includes error code and HTTP status for standardized API responses.
 */
public class MeghaConnectException extends RuntimeException {
    
    private final String errorCode;
    private final int httpStatus;
    private final String errorId; // Unique error tracking ID

    /**
     * Constructor with error code and HTTP status
     */
    public MeghaConnectException(String errorCode, String message, int httpStatus) {
        super(message);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
        this.errorId = generateErrorId();
    }

    /**
     * Constructor with error code, message, HTTP status, and cause
     */
    public MeghaConnectException(String errorCode, String message, int httpStatus, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
        this.errorId = generateErrorId();
    }

    /**
     * Constructor for backward compatibility (defaults to 400 Bad Request)
     */
    public MeghaConnectException(String message) {
        this(ErrorCodeConstants.GENERAL_ERROR, message, 400);
    }

    /**
     * Constructor for backward compatibility with cause
     */
    public MeghaConnectException(String message, Throwable cause) {
        this(ErrorCodeConstants.GENERAL_ERROR, message, 400, cause);
    }

    /**
     * Generate unique error tracking ID
     */
    private static String generateErrorId() {
        return "ERR-" + System.currentTimeMillis() + "-" + (int)(Math.random() * 10000);
    }

    // Getters
    public String getErrorCode() {
        return errorCode;
    }

    public int getHttpStatus() {
        return httpStatus;
    }

    public String getErrorId() {
        return errorId;
    }
}

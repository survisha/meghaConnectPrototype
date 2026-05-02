package com.survisha.meghaconnect.dto;

import java.time.LocalDateTime;

/**
 * Standard error response DTO for API responses.
 * All errors returned to the client follow this structure.
 */
public class ErrorResponseDto {
    
    private String errorCode;           // Error code (e.g., ERR_001)
    private String message;             // User-friendly error message
    private String errorId;             // Unique error tracking ID for debugging
    private Integer status;             // HTTP status code
    private String timestamp;           // Timestamp when error occurred
    private String path;                // Request path where error occurred
    private Integer remainingAttempts;  // For OTP-related errors
    private Integer waitTimeMinutes;    // For rate-limit errors
    private Object details;             // Additional error details (for validation errors, etc.)

    // Constructors
    public ErrorResponseDto() {
        this.timestamp = LocalDateTime.now().toString();
    }

    public ErrorResponseDto(String errorCode, String message) {
        this();
        this.errorCode = errorCode;
        this.message = message;
    }

    public ErrorResponseDto(String errorCode, String message, Integer status) {
        this();
        this.errorCode = errorCode;
        this.message = message;
        this.status = status;
    }

    public ErrorResponseDto(String errorCode, String message, String errorId, Integer status) {
        this();
        this.errorCode = errorCode;
        this.message = message;
        this.errorId = errorId;
        this.status = status;
    }

    // Getters and Setters
    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getErrorId() {
        return errorId;
    }

    public void setErrorId(String errorId) {
        this.errorId = errorId;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public Integer getRemainingAttempts() {
        return remainingAttempts;
    }

    public void setRemainingAttempts(Integer remainingAttempts) {
        this.remainingAttempts = remainingAttempts;
    }

    public Integer getWaitTimeMinutes() {
        return waitTimeMinutes;
    }

    public void setWaitTimeMinutes(Integer waitTimeMinutes) {
        this.waitTimeMinutes = waitTimeMinutes;
    }

    public Object getDetails() {
        return details;
    }

    public void setDetails(Object details) {
        this.details = details;
    }
}

package com.survisha.meghaconnect.response;

import com.survisha.meghaconnect.util.DateTimeUtil;
import com.survisha.meghaconnect.util.RequestContextUtil;

/**
 * Standard error response for API responses.
 */
public class ErrorResponse {

    private String errorCode;
    private String message;
    private String errorId;
    private Integer status;
    private String timestamp;
    private String path;
    private String requestId;
    private Integer remainingAttempts;
    private Integer attemptsRemaining;
    private Integer waitTimeMinutes;
    private Object details;

    public ErrorResponse() {
        this.timestamp = DateTimeUtil.nowIST().toString();
        this.requestId = RequestContextUtil.getRequestId();
    }

    public ErrorResponse(String errorCode, String message) {
        this();
        this.errorCode = errorCode;
        this.message = message;
    }

    public ErrorResponse(String errorCode, String message, Integer status) {
        this();
        this.errorCode = errorCode;
        this.message = message;
        this.status = status;
    }

    public ErrorResponse(String errorCode, String message, String errorId, Integer status) {
        this();
        this.errorCode = errorCode;
        this.message = message;
        this.errorId = errorId;
        this.status = status;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public String getCode() {
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

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public Integer getRemainingAttempts() {
        return remainingAttempts;
    }

    public void setRemainingAttempts(Integer remainingAttempts) {
        this.remainingAttempts = remainingAttempts;
        this.attemptsRemaining = remainingAttempts;
    }

    public Integer getAttemptsRemaining() {
        return attemptsRemaining;
    }

    public void setAttemptsRemaining(Integer attemptsRemaining) {
        this.attemptsRemaining = attemptsRemaining;
        this.remainingAttempts = attemptsRemaining;
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

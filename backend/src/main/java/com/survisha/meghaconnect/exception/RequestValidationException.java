package com.survisha.meghaconnect.exception;

public class RequestValidationException extends MeghaConnectException {

    public RequestValidationException(String errorCode, String message) {
        super(errorCode, message, 400);
    }

    public RequestValidationException(String errorCode, String message, int httpStatus) {
        super(errorCode, message, httpStatus);
    }
}

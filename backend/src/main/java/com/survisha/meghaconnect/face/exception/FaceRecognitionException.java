package com.survisha.meghaconnect.face.exception;

import com.survisha.meghaconnect.exception.MeghaConnectException;

public class FaceRecognitionException extends MeghaConnectException {
    public FaceRecognitionException(String code, String message, int status) {
        super(code, message, status);
    }

    public FaceRecognitionException(String code, String message, int status, Throwable cause) {
        super(code, message, status, cause);
    }
}

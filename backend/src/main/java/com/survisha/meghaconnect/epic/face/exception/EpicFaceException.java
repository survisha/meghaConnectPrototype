package com.survisha.meghaconnect.epic.face.exception;

import com.survisha.meghaconnect.exception.MeghaConnectException;

public class EpicFaceException extends MeghaConnectException {
    public EpicFaceException(String code, String message, int status) { super(code, message, status); }
    public EpicFaceException(String code, String message, int status, Throwable cause) { super(code, message, status, cause); }
}

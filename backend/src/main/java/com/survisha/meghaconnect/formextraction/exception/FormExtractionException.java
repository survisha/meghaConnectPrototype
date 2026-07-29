package com.survisha.meghaconnect.formextraction.exception;

import com.survisha.meghaconnect.exception.MeghaConnectException;

public class FormExtractionException extends MeghaConnectException {
    public FormExtractionException(String code, String message, int status) { super(code, message, status); }
    public FormExtractionException(String code, String message, int status, Throwable cause) { super(code, message, status, cause); }
}

package com.survisha.meghaconnect.dto;

import com.survisha.meghaconnect.response.ErrorResponse;

/**
 * Backward-compatible alias for the canonical error response model.
 */
public class ErrorResponseDto extends ErrorResponse {

    public ErrorResponseDto() {
        super();
    }

    public ErrorResponseDto(String errorCode, String message) {
        super(errorCode, message);
    }

    public ErrorResponseDto(String errorCode, String message, Integer status) {
        super(errorCode, message, status);
    }

    public ErrorResponseDto(String errorCode, String message, String errorId, Integer status) {
        super(errorCode, message, errorId, status);
    }
}

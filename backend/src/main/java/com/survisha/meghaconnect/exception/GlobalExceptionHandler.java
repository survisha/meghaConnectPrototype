package com.survisha.meghaconnect.exception;

import com.survisha.meghaconnect.dto.ErrorResponseDto;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // ========== CUSTOM MEGHACONNECT EXCEPTIONS ==========

    /**
     * Handle MeghaConnectException (base custom exception)
     */
    @ExceptionHandler(MeghaConnectException.class)
    public ResponseEntity<ErrorResponseDto> handleMeghaConnectException(MeghaConnectException ex, WebRequest request) {
        ErrorResponseDto error = new ErrorResponseDto(
            ex.getErrorCode(),
            ex.getMessage(),
            ex.getErrorId(),
            ex.getHttpStatus()
        );
        error.setPath(request.getDescription(false).replace("uri=", ""));
        return new ResponseEntity<>(error, HttpStatus.valueOf(ex.getHttpStatus()));
    }

    /**
     * Handle OTP validation failure with remaining attempts
     */
    @ExceptionHandler(OtpValidationFailedException.class)
    public ResponseEntity<ErrorResponseDto> handleOtpValidationFailed(OtpValidationFailedException ex, WebRequest request) {
        ErrorResponseDto error = new ErrorResponseDto(
            ex.getErrorCode(),
            ex.getMessage(),
            ex.getErrorId(),
            ex.getHttpStatus()
        );
        error.setRemainingAttempts(ex.getRemainingAttempts());
        error.setPath(request.getDescription(false).replace("uri=", ""));
        return new ResponseEntity<>(error, HttpStatus.valueOf(ex.getHttpStatus()));
    }

    /**
     * Handle OTP rate limit exceeded with wait time
     */
    @ExceptionHandler(OtpRateLimitExceededException.class)
    public ResponseEntity<ErrorResponseDto> handleOtpRateLimitExceeded(OtpRateLimitExceededException ex, WebRequest request) {
        ErrorResponseDto error = new ErrorResponseDto(
            ex.getErrorCode(),
            ex.getMessage(),
            ex.getErrorId(),
            ex.getHttpStatus()
        );
        error.setWaitTimeMinutes(ex.getWaitTimeMinutes());
        error.setPath(request.getDescription(false).replace("uri=", ""));
        return new ResponseEntity<>(error, HttpStatus.valueOf(ex.getHttpStatus()));
    }

    /**
     * Handle Scheduling conflict with conflict details
     */
    @ExceptionHandler(SchedulingConflictException.class)
    public ResponseEntity<ErrorResponseDto> handleSchedulingConflict(SchedulingConflictException ex, WebRequest request) {
        ErrorResponseDto error = new ErrorResponseDto(
            ex.getErrorCode(),
            ex.getMessage(),
            ex.getErrorId(),
            ex.getHttpStatus()
        );
        error.setDetails(ex.getConflictDateTime());
        error.setPath(request.getDescription(false).replace("uri=", ""));
        return new ResponseEntity<>(error, HttpStatus.valueOf(ex.getHttpStatus()));
    }

    /**
     * Handle External service errors
     */
    @ExceptionHandler(ExternalServiceException.class)
    public ResponseEntity<ErrorResponseDto> handleExternalServiceException(ExternalServiceException ex, WebRequest request) {
        ErrorResponseDto error = new ErrorResponseDto(
            ex.getErrorCode(),
            ex.getMessage(),
            ex.getErrorId(),
            ex.getHttpStatus()
        );
        error.setDetails(ex.getServiceName());
        error.setPath(request.getDescription(false).replace("uri=", ""));
        return new ResponseEntity<>(error, HttpStatus.valueOf(ex.getHttpStatus()));
    }

    /**
     * Handle EPIC name mismatch while preserving provider response details.
     */
    @ExceptionHandler(EpicNameMismatchException.class)
    public ResponseEntity<ErrorResponseDto> handleEpicNameMismatch(EpicNameMismatchException ex, WebRequest request) {
        ErrorResponseDto error = new ErrorResponseDto(
            ex.getErrorCode(),
            ex.getMessage(),
            ex.getErrorId(),
            ex.getHttpStatus()
        );
        error.setDetails(ex.getVerificationData());
        error.setPath(request.getDescription(false).replace("uri=", ""));
        return new ResponseEntity<>(error, HttpStatus.valueOf(ex.getHttpStatus()));
    }

    // ========== SPRING SECURITY EXCEPTIONS ==========

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponseDto> handleBadCredentials(BadCredentialsException ex, WebRequest request) {
        ErrorResponseDto error = new ErrorResponseDto(
            ErrorCodeConstants.INVALID_CREDENTIALS,
            ErrorCodeConstants.INVALID_CREDENTIALS_MSG,
            "SECURITY-" + System.nanoTime(),
            HttpStatus.UNAUTHORIZED.value()
        );
        error.setPath(request.getDescription(false).replace("uri=", ""));
        return new ResponseEntity<>(error, HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponseDto> handleAuthentication(AuthenticationException ex, WebRequest request) {
        ErrorResponseDto error = new ErrorResponseDto(
            ErrorCodeConstants.USER_NOT_AUTHENTICATED,
            ErrorCodeConstants.USER_NOT_AUTHENTICATED_MSG,
            "AUTH-" + System.nanoTime(),
            HttpStatus.UNAUTHORIZED.value()
        );
        error.setPath(request.getDescription(false).replace("uri=", ""));
        return new ResponseEntity<>(error, HttpStatus.UNAUTHORIZED);
    }

    // ========== BACKWARD COMPATIBLE EXCEPTIONS ==========

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponseDto> handleIllegalArgument(IllegalArgumentException ex, WebRequest request) {
        ErrorResponseDto error = new ErrorResponseDto(
            ErrorCodeConstants.GENERAL_ERROR,
            ex.getMessage() != null ? ex.getMessage() : ErrorCodeConstants.GENERAL_ERROR_MSG,
            "IAE-" + System.nanoTime(),
            HttpStatus.BAD_REQUEST.value()
        );
        error.setPath(request.getDescription(false).replace("uri=", ""));
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponseDto> handleIllegalState(IllegalStateException ex, WebRequest request) {
        ErrorResponseDto error = new ErrorResponseDto(
            ErrorCodeConstants.GENERAL_ERROR,
            ex.getMessage() != null ? ex.getMessage() : ErrorCodeConstants.GENERAL_ERROR_MSG,
            "ISE-" + System.nanoTime(),
            HttpStatus.CONFLICT.value()
        );
        error.setPath(request.getDescription(false).replace("uri=", ""));
        return new ResponseEntity<>(error, HttpStatus.CONFLICT);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponseDto> handleNotFound(ResourceNotFoundException ex, WebRequest request) {
        ErrorResponseDto error = new ErrorResponseDto(
            ErrorCodeConstants.GENERAL_ERROR,
            ex.getMessage() != null ? ex.getMessage() : "Resource not found",
            "RNF-" + System.nanoTime(),
            HttpStatus.NOT_FOUND.value()
        );
        error.setPath(request.getDescription(false).replace("uri=", ""));
        return new ResponseEntity<>(error, HttpStatus.NOT_FOUND);
    }

    // ========== VALIDATION EXCEPTIONS ==========

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponseDto> handleValidation(MethodArgumentNotValidException ex, WebRequest request) {
        Map<String, String> fieldErrors = new HashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.put(error.getField(), error.getDefaultMessage());
        }
        
        ErrorResponseDto response = new ErrorResponseDto(
            ErrorCodeConstants.GENERAL_ERROR,
            "Validation failed",
            "VALIDATION-" + System.nanoTime(),
            HttpStatus.BAD_REQUEST.value()
        );
        response.setDetails(fieldErrors);
        response.setPath(request.getDescription(false).replace("uri=", ""));
        
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    // ========== FILE UPLOAD EXCEPTIONS ==========

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponseDto> handleMaxUploadSize(MaxUploadSizeExceededException ex, WebRequest request) {
        ErrorResponseDto error = new ErrorResponseDto(
            ErrorCodeConstants.FILE_SIZE_EXCEEDED,
            ErrorCodeConstants.FILE_SIZE_EXCEEDED_MSG,
            "FILE-" + System.nanoTime(),
            HttpStatus.PAYLOAD_TOO_LARGE.value()
        );
        error.setPath(request.getDescription(false).replace("uri=", ""));
        return new ResponseEntity<>(error, HttpStatus.PAYLOAD_TOO_LARGE);
    }

    // ========== GENERIC EXCEPTION ==========

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseDto> handleGeneral(Exception ex, WebRequest request) {
        ErrorResponseDto error = new ErrorResponseDto(
            ErrorCodeConstants.GENERAL_ERROR,
            "An unexpected error occurred: " + (ex.getMessage() != null ? ex.getMessage() : "Unknown error"),
            "GENERAL-" + System.nanoTime(),
            HttpStatus.INTERNAL_SERVER_ERROR.value()
        );
        error.setPath(request.getDescription(false).replace("uri=", ""));
        
        // Log the full exception for debugging
        ex.printStackTrace();
        
        return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}

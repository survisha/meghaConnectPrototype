package com.survisha.meghaconnect.exception;

import com.survisha.meghaconnect.response.ErrorResponse;
import com.survisha.meghaconnect.util.RequestContextUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(MeghaConnectException.class)
    public ResponseEntity<ErrorResponse> handleMeghaConnectException(MeghaConnectException ex, WebRequest request) {
        ErrorResponse error = buildError(
                ex.getErrorCode(),
                clientSafeMessage(ex),
                ex.getErrorId(),
                ex.getHttpStatus(),
                request
        );
        logHandledException(error, ex);
        return ResponseEntity.status(ex.getHttpStatus()).body(error);
    }

    @ExceptionHandler(OtpValidationFailedException.class)
    public ResponseEntity<ErrorResponse> handleOtpValidationFailed(OtpValidationFailedException ex, WebRequest request) {
        ErrorResponse error = buildError(
                ex.getErrorCode(),
                ex.getMessage(),
                ex.getErrorId(),
                ex.getHttpStatus(),
                request
        );
        error.setRemainingAttempts(ex.getRemainingAttempts());
        logHandledException(error, ex);
        return ResponseEntity.status(ex.getHttpStatus()).body(error);
    }

    @ExceptionHandler(OtpRateLimitExceededException.class)
    public ResponseEntity<ErrorResponse> handleOtpRateLimitExceeded(OtpRateLimitExceededException ex, WebRequest request) {
        ErrorResponse error = buildError(
                ex.getErrorCode(),
                ex.getMessage(),
                ex.getErrorId(),
                ex.getHttpStatus(),
                request
        );
        error.setWaitTimeMinutes(ex.getWaitTimeMinutes());
        logHandledException(error, ex);
        return ResponseEntity.status(ex.getHttpStatus()).body(error);
    }

    @ExceptionHandler(SchedulingConflictException.class)
    public ResponseEntity<ErrorResponse> handleSchedulingConflict(SchedulingConflictException ex, WebRequest request) {
        ErrorResponse error = buildError(
                ex.getErrorCode(),
                ex.getMessage(),
                ex.getErrorId(),
                ex.getHttpStatus(),
                request
        );
        error.setDetails(ex.getConflictDateTime());
        logHandledException(error, ex);
        return ResponseEntity.status(ex.getHttpStatus()).body(error);
    }

    @ExceptionHandler(ExternalServiceException.class)
    public ResponseEntity<ErrorResponse> handleExternalServiceException(ExternalServiceException ex, WebRequest request) {
        ErrorResponse error = buildError(
                ex.getErrorCode(),
                RequestContextUtil.sanitizeForClient(ex.getMessage(), "External service is currently unavailable"),
                ex.getErrorId(),
                ex.getHttpStatus(),
                request
        );
        error.setDetails(ex.getServiceName());
        log.error("External service exception path={} service={} errorId={}",
                error.getPath(), ex.getServiceName(), ex.getErrorId(), ex);
        return ResponseEntity.status(ex.getHttpStatus()).body(error);
    }

    @ExceptionHandler(EpicNameMismatchException.class)
    public ResponseEntity<ErrorResponse> handleEpicNameMismatch(EpicNameMismatchException ex, WebRequest request) {
        ErrorResponse error = buildError(
                ex.getErrorCode(),
                ex.getMessage(),
                ex.getErrorId(),
                ex.getHttpStatus(),
                request
        );
        logHandledException(error, ex);
        return ResponseEntity.status(ex.getHttpStatus()).body(error);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentials(BadCredentialsException ex, WebRequest request) {
        ErrorResponse error = buildError(
                ErrorCodeConstants.INVALID_CREDENTIALS,
                ErrorCodeConstants.INVALID_CREDENTIALS_MSG,
                "SECURITY-" + System.nanoTime(),
                HttpStatus.UNAUTHORIZED.value(),
                request
        );
        logHandledException(error, ex);
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthentication(AuthenticationException ex, WebRequest request) {
        String errorCode = ErrorCodeConstants.USER_NOT_AUTHENTICATED;
        String message = ErrorCodeConstants.USER_NOT_AUTHENTICATED_MSG;
        HttpStatus status = HttpStatus.UNAUTHORIZED;
        if (ex instanceof LockedException) {
            errorCode = ErrorCodeConstants.USER_ACCOUNT_LOCKED;
            message = ErrorCodeConstants.USER_ACCOUNT_LOCKED_MSG;
            status = HttpStatus.LOCKED;
        } else if (ex instanceof DisabledException) {
            errorCode = ErrorCodeConstants.USER_ACCOUNT_INACTIVE;
            message = ErrorCodeConstants.USER_ACCOUNT_INACTIVE_MSG;
            status = HttpStatus.FORBIDDEN;
        }

        ErrorResponse error = buildError(
                errorCode,
                message,
                "AUTH-" + System.nanoTime(),
                status.value(),
                request
        );
        logHandledException(error, ex);
        return ResponseEntity.status(status).body(error);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex, WebRequest request) {
        ErrorResponse error = buildError(
                ErrorCodeConstants.GENERAL_ERROR,
                RequestContextUtil.sanitizeForClient(ex.getMessage(), ErrorCodeConstants.GENERAL_ERROR_MSG),
                "IAE-" + System.nanoTime(),
                HttpStatus.BAD_REQUEST.value(),
                request
        );
        logHandledException(error, ex);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleIllegalState(IllegalStateException ex, WebRequest request) {
        ErrorResponse error = buildError(
                ErrorCodeConstants.GENERAL_ERROR,
                RequestContextUtil.sanitizeForClient(ex.getMessage(), ErrorCodeConstants.GENERAL_ERROR_MSG),
                "ISE-" + System.nanoTime(),
                HttpStatus.CONFLICT.value(),
                request
        );
        logHandledException(error, ex);
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(ResourceNotFoundException ex, WebRequest request) {
        ErrorResponse error = buildError(
                ErrorCodeConstants.GENERAL_ERROR,
                RequestContextUtil.sanitizeForClient(ex.getMessage(), "Resource not found"),
                "RNF-" + System.nanoTime(),
                HttpStatus.NOT_FOUND.value(),
                request
        );
        logHandledException(error, ex);
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex, WebRequest request) {
        Map<String, String> fieldErrors = new HashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.put(error.getField(), error.getDefaultMessage());
        }

        ErrorResponse response = buildError(
                ErrorCodeConstants.GENERAL_ERROR,
                "Validation failed",
                "VALIDATION-" + System.nanoTime(),
                HttpStatus.BAD_REQUEST.value(),
                request
        );
        response.setDetails(fieldErrors);
        logHandledException(response, ex);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(BindException.class)
    public ResponseEntity<ErrorResponse> handleBindException(BindException ex, WebRequest request) {
        Map<String, String> fieldErrors = new HashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.put(error.getField(), error.getDefaultMessage());
        }

        ErrorResponse response = buildError(
                ErrorCodeConstants.GENERAL_ERROR,
                "Validation failed",
                "BIND-" + System.nanoTime(),
                HttpStatus.BAD_REQUEST.value(),
                request
        );
        response.setDetails(fieldErrors);
        logHandledException(response, ex);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponse> handleMaxUploadSize(MaxUploadSizeExceededException ex, WebRequest request) {
        ErrorResponse error = buildError(
                ErrorCodeConstants.FILE_SIZE_EXCEEDED,
                ErrorCodeConstants.FILE_SIZE_EXCEEDED_MSG,
                "FILE-" + System.nanoTime(),
                HttpStatus.PAYLOAD_TOO_LARGE.value(),
                request
        );
        logHandledException(error, ex);
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(error);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneral(Exception ex, WebRequest request) {
        ErrorResponse error = buildError(
                ErrorCodeConstants.UNEXPECTED_ERROR,
                ErrorCodeConstants.UNEXPECTED_ERROR_MSG,
                "GENERAL-" + System.nanoTime(),
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                request
        );
        log.error("Unhandled exception path={} errorId={}", error.getPath(), error.getErrorId(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }

    private ErrorResponse buildError(String errorCode,
                                     String message,
                                     String errorId,
                                     int status,
                                     WebRequest request) {
        ErrorResponse error = new ErrorResponse(errorCode, message, errorId, status);
        error.setPath(resolvePath(request));
        error.setRequestId(RequestContextUtil.getRequestId());
        return error;
    }

    private String clientSafeMessage(MeghaConnectException ex) {
        if (ex instanceof VisitorRegistrationValidationException) {
            return ex.getMessage();
        }
        if (ErrorCodeConstants.INVALID_CREDENTIALS.equals(ex.getErrorCode())) {
            return ErrorCodeConstants.INVALID_CREDENTIALS_MSG;
        }
        if (ErrorCodeConstants.USER_ACCOUNT_LOCKED.equals(ex.getErrorCode())) {
            return ErrorCodeConstants.USER_ACCOUNT_LOCKED_MSG;
        }
        if (ErrorCodeConstants.USER_ACCOUNT_INACTIVE.equals(ex.getErrorCode())) {
            return ErrorCodeConstants.USER_ACCOUNT_INACTIVE_MSG;
        }
        return RequestContextUtil.sanitizeForClient(ex.getMessage(), ErrorCodeConstants.GENERAL_ERROR_MSG);
    }

    private String resolvePath(WebRequest request) {
        return request.getDescription(false).replace("uri=", "");
    }

    private void logHandledException(ErrorResponse error, Exception ex) {
        String message = RequestContextUtil.sanitizeForLog(ex.getMessage());
        if (error.getStatus() != null && error.getStatus() >= 500) {
            log.error("Handled exception status={} code={} path={} errorId={} message={}",
                    error.getStatus(), error.getErrorCode(), error.getPath(), error.getErrorId(), message, ex);
        } else {
            log.warn("Handled exception status={} code={} path={} errorId={} message={}",
                    error.getStatus(), error.getErrorCode(), error.getPath(), error.getErrorId(), message);
        }
    }
}

package com.mehmetkatr.project_easy_translate.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AuthenticationFailedException.class)
    public ResponseEntity<Map<String, Object>> handleAuthenticationFailed(AuthenticationFailedException exception) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                "code", "INVALID_CREDENTIALS",
                "error", exception.getMessage()
        ));
    }

    @ExceptionHandler(EmailNotVerifiedException.class)
    public ResponseEntity<Map<String, Object>> handleEmailNotVerified(EmailNotVerifiedException exception) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                "code", "EMAIL_NOT_VERIFIED",
                "error", exception.getMessage(),
                "email", exception.getEmail()
        ));
    }

    @ExceptionHandler(InvalidCodeException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidCode(InvalidCodeException exception) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                "code", "INVALID_OR_EXPIRED_CODE",
                "error", exception.getMessage()
        ));
    }


    @ExceptionHandler(MailDeliveryException.class)
    public ResponseEntity<Map<String, Object>> handleMailDelivery(MailDeliveryException exception) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of(
                "code", "MAIL_DELIVERY_FAILED",
                "error", exception.getMessage()
        ));
    }

    @ExceptionHandler(ResourceConflictException.class)
    public ResponseEntity<Map<String, Object>> handleResourceConflict(ResourceConflictException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                "code", exception.getCode(),
                "error", exception.getMessage()
        ));
    }

    @ExceptionHandler(AccountPendingDeletionException.class)
    public ResponseEntity<Map<String, Object>> handlePendingDeletion(AccountPendingDeletionException exception) {
        return ResponseEntity.status(HttpStatus.LOCKED).body(Map.of(
                "code", "ACCOUNT_PENDING_DELETION",
                "error", exception.getMessage(),
                "retryAt", exception.getRetryAt().toString()
        ));
    }

    @ExceptionHandler(RateLimitExceededException.class)
    public ResponseEntity<Map<String, Object>> handleRateLimit(RateLimitExceededException exception) {
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(Map.of(
                "code", "RATE_LIMITED",
                "error", exception.getMessage(),
                "retryAfterSeconds", exception.getRetryAfterSeconds()
        ));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException exception) {
        return ResponseEntity.badRequest().body(Map.of(
                "code", "BAD_REQUEST",
                "error", exception.getMessage()
        ));
    }

    @ExceptionHandler(DailyStoryLimitExceededException.class)
    public ResponseEntity<Map<String, Object>> handleDailyStoryLimit(DailyStoryLimitExceededException exception) {
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(Map.of(
                "code", "STORY_DAILY_LIMIT_REACHED",
                "error", exception.getMessage(),
                "dailyLimit", exception.getDailyLimit(),
                "usedToday", exception.getUsedToday(),
                "resetAt", exception.getResetAt().toString()
        ));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleAccessDenied(AccessDeniedException exception) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                "code", "ACCESS_DENIED",
                "error", exception.getMessage()
        ));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException exception) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        for (FieldError fieldError : exception.getBindingResult().getFieldErrors()) {
            fieldErrors.put(fieldError.getField(), fieldError.getDefaultMessage());
        }

        return ResponseEntity.badRequest().body(Map.of(
                "code", "VALIDATION_ERROR",
                "error", "Validation failed",
                "fields", fieldErrors
        ));
    }
}

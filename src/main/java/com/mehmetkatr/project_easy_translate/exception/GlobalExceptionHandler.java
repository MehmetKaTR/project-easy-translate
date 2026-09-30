package com.mehmetkatr.project_easy_translate.exception;

import com.mehmetkatr.project_easy_translate.config.TraceIdFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(AuthenticationFailedException.class)
    public ResponseEntity<Map<String, Object>> handleAuthenticationFailed(AuthenticationFailedException exception) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                "code", "INVALID_CREDENTIALS",
                "error", exception.getMessage()
        ));
    }

    @ExceptionHandler(InvalidRefreshTokenException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidRefreshToken(InvalidRefreshTokenException exception) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                "code", "INVALID_REFRESH_TOKEN",
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

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Object>> handleDataIntegrity(DataIntegrityViolationException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                "code", "DATA_INTEGRITY_VIOLATION",
                "error", "Delete failed because related records still exist."
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

    /**
     * Son çare: yukarıdaki özel handler'ların hiçbirine uymayan beklenmeyen hatalar.
     * İç detayları (stacktrace) istemciye SIZDIRMAZ; sadece bir traceId döner.
     * Tam hata, aynı traceId ile sunucu loguna yazılır → destek/hata ayıklama için.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleUnexpected(Exception exception) {
        // İsteğin MDC traceId'sini kullan (TraceIdFilter koydu) → istemciye dönen id loglarla eşleşir.
        String traceId = MDC.get(TraceIdFilter.TRACE_ID);
        if (traceId == null || traceId.isBlank()) {
            traceId = UUID.randomUUID().toString();
        }
        log.error("Beklenmeyen hata [traceId={}]", traceId, exception);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "code", "INTERNAL_ERROR",
                "error", "Beklenmeyen bir hata oluştu. Lütfen destek ile iletişime geçin.",
                "traceId", traceId
        ));
    }
}

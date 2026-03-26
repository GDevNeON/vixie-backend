package com.neong.vixie.helpers.api;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Global exception handler for the AI Companion service.
 * Returns structured error responses matching user-auth format.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationErrors(
            MethodArgumentNotValidException ex) {
        Map<String, String> fieldErrors = new HashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.put(error.getField(), error.getDefaultMessage());
        }

        Map<String, Object> body = Map.of(
                "error", "VALIDATION_ERROR",
                "message", "One or more fields failed validation",
                "field_errors", fieldErrors,
                "timestamp", Instant.now().toString(),
                "correlation_id", UUID.randomUUID().toString()
        );

        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(body);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleAccessDenied(AccessDeniedException ex) {
        Map<String, Object> body = Map.of(
                "error", "ACCESS_DENIED",
                "message", "You do not have permission to access this resource",
                "timestamp", Instant.now().toString(),
                "correlation_id", UUID.randomUUID().toString()
        );

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(body);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericError(Exception ex) {
        String correlationId = UUID.randomUUID().toString();
        log.error("Unhandled exception [correlation_id={}]: {}", correlationId, ex.getMessage(), ex);

        Map<String, Object> body = Map.of(
                "error", "INTERNAL_ERROR",
                "message", "An unexpected error occurred. Please try again later.",
                "timestamp", Instant.now().toString(),
                "correlation_id", correlationId
        );

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }
}

package com.neong.vixie.models.dto;

import java.time.Instant;
import java.util.Map;
import org.slf4j.MDC;

public record ErrorResponse(
        String error,
        String message,
        Map<String, String> fieldErrors,
        String timestamp,
        String correlationId
) {
    public static ErrorResponse of(String error, String message) {
        return new ErrorResponse(
                error,
                message,
                null,
                Instant.now().toString(),
                MDC.get("X-Correlation-Id")
        );
    }

    public static ErrorResponse ofValidation(String message, Map<String, String> fieldErrors) {
        return new ErrorResponse(
                "VALIDATION_ERROR",
                message,
                fieldErrors,
                Instant.now().toString(),
                MDC.get("X-Correlation-Id")
        );
    }
}

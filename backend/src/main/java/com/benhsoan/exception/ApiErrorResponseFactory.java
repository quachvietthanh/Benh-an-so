package com.benhsoan.exception;

import java.time.Instant;
import java.util.Map;

import org.springframework.http.HttpStatus;

public final class ApiErrorResponseFactory {

    private ApiErrorResponseFactory() {
    }

    public static ApiErrorResponse create(
            HttpStatus status,
            String code,
            String message,
            String path
    ) {
        return create(status, code, message, path, Map.of());
    }

    public static ApiErrorResponse create(
            HttpStatus status,
            String code,
            String message,
            String path,
            Map<String, Object> details
    ) {
        return new ApiErrorResponse(
                Instant.now(),
                status.value(),
                status.getReasonPhrase(),
                code,
                message,
                path,
                details
        );
    }
}

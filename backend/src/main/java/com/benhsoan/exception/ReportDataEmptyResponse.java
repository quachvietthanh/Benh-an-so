package com.benhsoan.exception;

import java.time.Instant;

public record ReportDataEmptyResponse(
        Instant timestamp,
        int status,
        String error,
        String code,
        String message,
        String path
) {
}

package com.benhsoan.application.ucservice.backup;

import org.springframework.stereotype.Component;

/**
 * Produces a safe, bounded failure reason for persistence and audit.
 * It never includes stack traces or sensitive connection/credential data;
 * only the root exception message is retained, truncated to a fixed limit.
 */
@Component
public class BackupFailureReason {

    private static final int MAX_LENGTH = 500;

    public String sanitize(Throwable throwable) {
        String message = rootMessage(throwable);
        if (message == null || message.isBlank()) {
            message = "Unknown failure";
        }
        if (message.length() > MAX_LENGTH) {
            message = message.substring(0, MAX_LENGTH - 1) + "\u2026";
        }
        return message;
    }

    private String rootMessage(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current.getMessage() != null && !current.getMessage().isBlank()) {
                return current.getMessage();
            }
            current = current.getCause();
        }
        return throwable == null ? null : throwable.getClass().getSimpleName();
    }
}

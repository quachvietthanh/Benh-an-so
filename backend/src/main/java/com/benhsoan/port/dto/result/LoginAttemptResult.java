package com.benhsoan.port.dto.result;

import java.time.Instant;

public record LoginAttemptResult(
        int attemptCount,
        boolean blocked,
        boolean newlyBlocked,
        Instant blockedUntil,
        long retryAfterSeconds
) {
}

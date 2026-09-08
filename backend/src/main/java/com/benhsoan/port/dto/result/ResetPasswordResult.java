package com.benhsoan.port.dto.result;

import java.time.Instant;
import java.util.UUID;

public record ResetPasswordResult(
        UUID userId,
        String username,
        String temporaryPassword,
        Instant resetAt
) {
}

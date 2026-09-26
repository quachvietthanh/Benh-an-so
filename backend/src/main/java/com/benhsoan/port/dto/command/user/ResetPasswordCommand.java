package com.benhsoan.port.dto.command.user;

import java.util.UUID;

public record ResetPasswordCommand(
        UUID targetUserId,
        String temporaryPassword,
        Integer expiresInHours
) {
    public ResetPasswordCommand(UUID targetUserId, String temporaryPassword) {
        this(targetUserId, temporaryPassword, null);
    }
}


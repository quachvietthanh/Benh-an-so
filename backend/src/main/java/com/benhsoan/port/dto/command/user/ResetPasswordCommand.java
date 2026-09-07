package com.benhsoan.port.dto.command.user;

import java.util.UUID;

public record ResetPasswordCommand(
        UUID targetUserId,
        String temporaryPassword
) {
}

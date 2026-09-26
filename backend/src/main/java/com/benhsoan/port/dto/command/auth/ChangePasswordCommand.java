package com.benhsoan.port.dto.command.auth;

import java.util.UUID;

public record ChangePasswordCommand(
        UUID userId,
        String oldPassword,
        String newPassword
) {
}

package com.benhsoan.infrastructure.authSecurity;

import java.util.UUID;

public record CurrentUserPrincipal(
        UUID userId,
        String username,
        UUID sessionId
) {
    public CurrentUserPrincipal(UUID userId, String username) {
        this(userId, username, null);
    }
}
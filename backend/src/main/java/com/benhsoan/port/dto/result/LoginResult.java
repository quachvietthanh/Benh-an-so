package com.benhsoan.port.dto.result;

import java.time.Instant;
import java.util.UUID;

public record LoginResult(

        UUID userId,

        String username,

        String accessToken,

        String refreshToken,

        String role,

        Instant expiredAt,

        boolean mustChangePassword

) {
    public LoginResult(
            UUID userId,
            String username,
            String accessToken,
            String refreshToken,
            String role,
            Instant expiredAt
    ) {
        this(userId, username, accessToken, refreshToken, role, expiredAt, false);
    }
}

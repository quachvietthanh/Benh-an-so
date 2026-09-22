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

        boolean mustChangePassword,

        boolean twoFactorRequired,

        UUID twoFactorToken,

        Instant twoFactorExpiresAt

) {
    public LoginResult(
            UUID userId,
            String username,
            String accessToken,
            String refreshToken,
            String role,
            Instant expiredAt,
            boolean mustChangePassword
    ) {
        this(userId, username, accessToken, refreshToken, role, expiredAt, mustChangePassword, false, null, null);
    }

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

    public static LoginResult twoFactorRequired(
            UUID userId,
            String username,
            String role,
            UUID twoFactorToken,
            Instant twoFactorExpiresAt
    ) {
        return new LoginResult(userId, username, null, null, role, null, false, true, twoFactorToken, twoFactorExpiresAt);
    }
}

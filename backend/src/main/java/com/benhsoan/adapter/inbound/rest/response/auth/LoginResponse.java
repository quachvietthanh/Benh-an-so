package com.benhsoan.adapter.inbound.rest.response.auth;

import java.time.Instant;
import java.util.UUID;

public record LoginResponse(

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
    public LoginResponse(
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

    public LoginResponse(
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

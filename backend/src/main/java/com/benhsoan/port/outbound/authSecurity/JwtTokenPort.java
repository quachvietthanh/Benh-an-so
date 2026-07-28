package com.benhsoan.port.outbound.authSecurity;

import java.time.Instant;
import java.util.UUID;

public interface JwtTokenPort {

    String generateToken(
            UUID userId,
            UUID sessionId,
            String username,
            String role
    );

    UUID getUserId(String token);

    String getUsername(String token);

    String getRole(String token);

    UUID getSessionId(String token);

    Instant getExpiredAt(String token);

    boolean isExpired(String token);

    boolean validate(String token);

}

package com.benhsoan.infrastructure.authSecurity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class JwtTokenAdapterTest {

    private static final long ACCESS_TOKEN_EXPIRATION_MS = Duration.ofMinutes(15).toMillis();

    private final JwtTokenAdapter jwtTokenAdapter = new JwtTokenAdapter(
            "test-secret-key-must-have-at-least-32-bytes",
            ACCESS_TOKEN_EXPIRATION_MS
    );

    @Test
    void generatesShortLivedTokenBoundToUserSessionAndRole() {
        UUID userId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        Instant beforeGeneration = Instant.now();

        String token = jwtTokenAdapter.generateToken(userId, sessionId, "admin", "ADMIN");

        assertTrue(jwtTokenAdapter.validate(token));
        assertEquals(userId, jwtTokenAdapter.getUserId(token));
        assertEquals(sessionId, jwtTokenAdapter.getSessionId(token));
        assertEquals("ADMIN", jwtTokenAdapter.getRole(token));
        assertFalse(jwtTokenAdapter.getExpiredAt(token).isAfter(
                beforeGeneration.plus(Duration.ofMinutes(15).plusSeconds(1))));
    }
}

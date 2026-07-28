package com.benhsoan.domain.auth;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("UserSession - Session Security Tests")
class UserSessionTest {

    @Test
    @DisplayName("Active session should not be marked expired")
    void activeSessionNotExpired() {
        UserSession session = UserSession.create(
                UUID.randomUUID(),
                "token-hash",
                Instant.now().plusSeconds(3600)
        );

        assertFalse(session.isRefreshExpired(Instant.now()));
        assertTrue(session.isActive(Instant.now(), Duration.ofMinutes(30)));
    }

    @Test
    @DisplayName("Expired session should be detected")
    void expiredSession() {
        UserSession session = UserSession.create(
                UUID.randomUUID(),
                "token-hash",
                Instant.now().minusSeconds(60)
        );

        assertTrue(session.isRefreshExpired(Instant.now()));
    }

    @Test
    @DisplayName("Revoked session should not be active")
    void revokedSession() {
        UserSession session = UserSession.create(
                UUID.randomUUID(),
                "token-hash",
                Instant.now().plusSeconds(3600)
        );

        assertTrue(session.isActive(Instant.now(), Duration.ofMinutes(30)));
        session.revoke(Instant.now());
        assertTrue(session.isRevoked());
        assertFalse(session.isActive(Instant.now(), Duration.ofMinutes(30)));
    }

    @Test
    @DisplayName("Refresh should update last used and expiry")
    void refreshSession() {
        UserSession session = UserSession.create(
                UUID.randomUUID(),
                "old-token-hash",
                Instant.now().minusSeconds(60)
        );

        Duration timeout = Duration.ofHours(1);
        session.refresh(timeout);

        assertFalse(session.isRefreshExpired(Instant.now()));
        assertTrue(session.isActive(Instant.now(), Duration.ofMinutes(30)));
    }

    @Test
    @DisplayName("Refresh-token rotation retains the previous hash for reuse detection")
    void rotateRefreshToken() {
        UserSession session = UserSession.create(
                UUID.randomUUID(), "old-hash", Instant.now().plusSeconds(60)
        );
        Instant expiresAt = Instant.now().plusSeconds(3600);

        session.rotateRefreshToken("new-hash", expiresAt, Instant.now());

        assertEquals("new-hash", session.getRefreshTokenHash());
        assertEquals("old-hash", session.getPreviousRefreshTokenHash());
        assertTrue(session.matchesPreviousRefreshTokenHash("old-hash"));
        assertEquals(expiresAt, session.getRefreshExpiresAt());
    }

    @Test
    @DisplayName("Idle timeout should expire inactive session")
    void idleTimeoutExpires() {
        UserSession session = UserSession.create(
                UUID.randomUUID(),
                "token-hash",
                Instant.now().plusSeconds(3600)
        );

        Duration idleTimeout = Duration.ofMinutes(15);
        assertTrue(session.isActive(Instant.now(), idleTimeout));

        // Simulate idle timeout by updating lastUsed to far in the past
        session.updateLastUsed(Instant.now().minusSeconds(1800));

        assertTrue(session.isIdleTimeout(Instant.now(), idleTimeout));
        assertFalse(session.isActive(Instant.now(), idleTimeout));
    }

    @Test
    @DisplayName("Session restore should recreate from persistence")
    void sessionRestore() {
        UUID id = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        String refreshTokenHash = "restored-hash";
        String previousRefreshTokenHash = "previous-restored-hash";
        Instant refreshExpiresAt = Instant.now().plusSeconds(3600);
        Instant createdAt = Instant.now().minusSeconds(86400);
        Instant lastUsedAt = Instant.now().minusSeconds(3600);
        Instant revokedAt = null;

        UserSession session = UserSession.restore(
                id, userId, refreshTokenHash, previousRefreshTokenHash, refreshExpiresAt, createdAt, lastUsedAt, revokedAt
        );

        assertEquals(id, session.getId());
        assertEquals(userId, session.getUserId());
        assertEquals(refreshTokenHash, session.getRefreshTokenHash());
        assertEquals(previousRefreshTokenHash, session.getPreviousRefreshTokenHash());
        assertEquals(refreshExpiresAt, session.getRefreshExpiresAt());
        assertEquals(createdAt, session.getCreatedAt());
        assertEquals(lastUsedAt, session.getLastUsedAt());
        assertFalse(session.isRevoked());
    }
}

package com.benhsoan.domain.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class TwoFactorChallengeTest {

    private static final Instant NOW = Instant.parse("2026-09-22T10:00:00Z");

    @Test
    void newChallengeIsValidAndNotConsumed() {
        TwoFactorChallenge challenge = TwoFactorChallenge.create(UUID.randomUUID(), "hash", NOW.plusSeconds(300), NOW);

        assertFalse(challenge.isConsumed());
        assertFalse(challenge.isExpired(NOW));
        assertTrue(challenge.isValid(NOW));
    }

    @Test
    void expiresAfterTtl() {
        TwoFactorChallenge challenge = TwoFactorChallenge.create(UUID.randomUUID(), "hash", NOW.plusSeconds(300), NOW);

        assertTrue(challenge.isExpired(NOW.plusSeconds(301)));
        assertFalse(challenge.isValid(NOW.plusSeconds(301)));
    }

    @Test
    void consumedChallengeIsNotValid() {
        TwoFactorChallenge challenge = TwoFactorChallenge.create(UUID.randomUUID(), "hash", NOW.plusSeconds(300), NOW);
        challenge.markConsumed(NOW);

        assertTrue(challenge.isConsumed());
        assertFalse(challenge.isValid(NOW));
    }

    @Test
    void rotateCodeResetsAttemptsAndExtendsExpiry() {
        TwoFactorChallenge challenge = TwoFactorChallenge.create(UUID.randomUUID(), "hash", NOW.plusSeconds(300), NOW);
        challenge.incrementAttempts();
        challenge.incrementAttempts();

        Instant newExpiry = NOW.plusSeconds(600);
        challenge.rotateCode("newHash", newExpiry);

        assertEquals(0, challenge.getAttempts());
        assertEquals(newExpiry, challenge.getExpiresAt());
        assertEquals("newHash", challenge.getCodeHash());
        assertFalse(challenge.isExpired(NOW.plusSeconds(599)));
    }
}

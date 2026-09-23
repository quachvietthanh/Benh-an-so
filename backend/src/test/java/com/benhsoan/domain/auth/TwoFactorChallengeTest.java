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
        assertFalse(challenge.isAttemptsExceeded());
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
    void attemptsExceededAtMaxAttempts() {
        TwoFactorChallenge challenge = TwoFactorChallenge.create(UUID.randomUUID(), "hash", NOW.plusSeconds(300), NOW);
        for (int i = 0; i < TwoFactorChallenge.MAX_ATTEMPTS; i++) {
            challenge.incrementAttempts();
        }

        assertTrue(challenge.isAttemptsExceeded());
        assertFalse(challenge.isValid(NOW));
    }

    @Test
    void notAttemptsExceededBelowMax() {
        TwoFactorChallenge challenge = TwoFactorChallenge.create(UUID.randomUUID(), "hash", NOW.plusSeconds(300), NOW);
        for (int i = 0; i < TwoFactorChallenge.MAX_ATTEMPTS - 1; i++) {
            challenge.incrementAttempts();
        }

        assertFalse(challenge.isAttemptsExceeded());
        assertTrue(challenge.isValid(NOW));
    }

    @Test
    void rotateCodeDoesNotResetAttemptsOrCreationTime() {
        TwoFactorChallenge challenge = TwoFactorChallenge.create(UUID.randomUUID(), "hash", NOW.plusSeconds(300), NOW);
        challenge.incrementAttempts();
        challenge.incrementAttempts();
        Instant originalCreatedAt = challenge.getCreatedAt();

        Instant newExpiry = NOW.plusSeconds(600);
        challenge.rotateCode("newHash", newExpiry);

        assertEquals(2, challenge.getAttempts(), "Resend must not reset the cumulative attempt count");
        assertEquals(newExpiry, challenge.getExpiresAt());
        assertEquals("newHash", challenge.getCodeHash());
        assertEquals(originalCreatedAt, challenge.getCreatedAt(), "Resend must not reset the original creation time");
    }

    @Test
    void lifetimeExceededAfterFifteenMinutesFromCreation() {
        TwoFactorChallenge challenge = TwoFactorChallenge.create(UUID.randomUUID(), "hash", NOW.plusSeconds(300), NOW);

        assertFalse(challenge.isLifetimeExceeded(NOW.plusSeconds(TwoFactorChallenge.MAX_LIFETIME_SECONDS - 1)));
        assertTrue(challenge.isLifetimeExceeded(NOW.plusSeconds(TwoFactorChallenge.MAX_LIFETIME_SECONDS + 1)));
    }
}

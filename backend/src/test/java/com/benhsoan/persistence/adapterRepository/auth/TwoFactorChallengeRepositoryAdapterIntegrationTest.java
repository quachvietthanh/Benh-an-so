package com.benhsoan.persistence.adapterRepository.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import com.benhsoan.domain.auth.Role;
import com.benhsoan.domain.auth.TwoFactorChallenge;
import com.benhsoan.domain.auth.User;
import com.benhsoan.port.outbound.repository.auth.RoleRepository;
import com.benhsoan.port.outbound.repository.auth.TwoFactorChallengeRepository;
import com.benhsoan.port.outbound.repository.auth.UserRepository;

import jakarta.transaction.Transactional;

@SpringBootTest(properties = {
    "spring.flyway.enabled=false",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.url=jdbc:h2:mem:twofactor_repo_testdb;DB_CLOSE_DELAY=-1;MODE=MYSQL",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.jpa.defer-datasource-initialization=true"
})
@Transactional
@ActiveProfiles("test")
@DisplayName("Two-Factor Challenge Repository Integration Tests")
class TwoFactorChallengeRepositoryAdapterIntegrationTest {

    @Autowired
    private TwoFactorChallengeRepository challengeRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    private User testUser;

    @BeforeEach
    void setUp() {
        Role doctorRole = roleRepository.findByName("DOCTOR")
                .orElseGet(() -> roleRepository.save(Role.create("DOCTOR", "Doctor role", true, Set.of())));
        testUser = userRepository.save(User.create(
                "twofactor_repo_user",
                "hashed_pass",
                "Doctor Test",
                "twofactor_repo@test.com",
                null,
                doctorRole.getId()
        ));
    }

    private TwoFactorChallenge saveChallenge() {
        Instant now = Instant.now();
        return challengeRepository.save(TwoFactorChallenge.create(
                testUser.getId(), "hashed_code", now.plusSeconds(300), now));
    }

    @Test
    @DisplayName("markConsumed is single-use: first returns 1, second returns 0")
    void markConsumedIsSingleUse() {
        TwoFactorChallenge challenge = saveChallenge();
        Instant now = Instant.now();

        int first = challengeRepository.markConsumed(challenge.getId(), now);
        int second = challengeRepository.markConsumed(challenge.getId(), now.plusSeconds(1));

        assertEquals(1, first);
        assertEquals(0, second);
    }

    @Test
    @DisplayName("incrementAttempts is bounded at MAX_ATTEMPTS")
    void incrementAttemptsIsBoundedAtMax() {
        TwoFactorChallenge challenge = saveChallenge();
        int max = TwoFactorChallenge.MAX_ATTEMPTS;

        for (int i = 0; i < max; i++) {
            assertEquals(1, challengeRepository.incrementAttempts(challenge.getId(), max),
                    "Attempt " + (i + 1) + " should increment");
        }
        // Beyond the limit the bounded update affects zero rows.
        assertEquals(0, challengeRepository.incrementAttempts(challenge.getId(), max));
    }

    @Test
    @DisplayName("invalidatePendingChallengesByUserId consumes all pending challenges for the user only")
    void invalidatePendingChallengesByUserId_consumesOnlyThatUsersPendingChallenges() {
        Instant now = Instant.now();
        TwoFactorChallenge a = challengeRepository.save(TwoFactorChallenge.create(
                testUser.getId(), "hash_a", now.plusSeconds(300), now));
        TwoFactorChallenge b = challengeRepository.save(TwoFactorChallenge.create(
                testUser.getId(), "hash_b", now.plusSeconds(300), now));

        challengeRepository.invalidatePendingChallengesByUserId(testUser.getId(), now);

        // Both pending challenges are now consumed (single-use markConsumed returns 0).
        assertEquals(0, challengeRepository.markConsumed(a.getId(), now));
        assertEquals(0, challengeRepository.markConsumed(b.getId(), now));
    }

    @Test
    @DisplayName("invalidatePendingChallengesByUserId does not invalidate another user's challenge")
    void invalidatePendingChallengesByUserId_doesNotAffectOtherUser() {
        Role otherRole = roleRepository.findByName("ADMIN")
                .orElseGet(() -> roleRepository.save(Role.create("ADMIN", "Admin role", true, Set.of())));
        User other = userRepository.save(User.create(
                "other_user", "hash", "Other", "other@test.com", null, otherRole.getId()));

        Instant now = Instant.now();
        TwoFactorChallenge otherChallenge = challengeRepository.save(TwoFactorChallenge.create(
                other.getId(), "hash_other", now.plusSeconds(300), now));

        challengeRepository.invalidatePendingChallengesByUserId(testUser.getId(), now);

        // The other user's challenge remains consumable.
        assertEquals(1, challengeRepository.markConsumed(otherChallenge.getId(), now));
    }

    private static final Instant CLEANUP_NOW = Instant.parse("2026-10-01T00:00:00Z");

    private TwoFactorChallenge saveChallenge(UUID userId, String hash, Instant expiresAt, int attempts, Instant consumedAt, Instant createdAt) {
        return challengeRepository.save(TwoFactorChallenge.restore(
                UUID.randomUUID(), userId, hash, expiresAt, attempts, consumedAt, createdAt));
    }

    @Test
    @DisplayName("Cleanup deletes only consumed/expired challenges older than retention, never active ones")
    void deleteExpiredOrConsumedBefore_deletesOnlyUnusableOldChallenges() {
        Instant threshold = CLEANUP_NOW.minus(Duration.ofDays(30));
        long day = 86400L;

        // C1: expired + old -> deleted
        saveChallenge(testUser.getId(), "expired_old", CLEANUP_NOW.minusSeconds(day), 0, null, CLEANUP_NOW.minusSeconds(31 * day));
        // C2: consumed + old -> deleted
        saveChallenge(testUser.getId(), "consumed_old", CLEANUP_NOW.plusSeconds(3600), 0, CLEANUP_NOW.minusSeconds(31 * day), CLEANUP_NOW.minusSeconds(31 * day));

        // C3: expired + recent -> retained
        TwoFactorChallenge expiredRecent = saveChallenge(testUser.getId(), "expired_recent", CLEANUP_NOW.minusSeconds(3600), 0, null, CLEANUP_NOW.minusSeconds(3600));
        // C4: consumed + recent -> retained
        TwoFactorChallenge consumedRecent = saveChallenge(testUser.getId(), "consumed_recent", CLEANUP_NOW.plusSeconds(3600), 0, CLEANUP_NOW.minusSeconds(1800), CLEANUP_NOW.minusSeconds(3600));
        // C5: active (future expiry, not consumed) -> retained even though createdAt is old
        TwoFactorChallenge activeOld = saveChallenge(testUser.getId(), "active_old", CLEANUP_NOW.plusSeconds(3600), 0, null, CLEANUP_NOW.minusSeconds(31 * day));

        int deleted = challengeRepository.deleteExpiredOrConsumedBefore(threshold, CLEANUP_NOW);

        assertEquals(2, deleted);
        assertTrue(challengeRepository.findById(expiredRecent.getId()).isPresent());
        assertTrue(challengeRepository.findById(consumedRecent.getId()).isPresent());
        assertTrue(challengeRepository.findById(activeOld.getId()).isPresent());
    }

    @Test
    @DisplayName("Cleanup is idempotent: second run deletes nothing")
    void cleanupIsIdempotent() {
        Instant threshold = CLEANUP_NOW.minus(Duration.ofDays(30));
        long day = 86400L;
        saveChallenge(testUser.getId(), "expired_old", CLEANUP_NOW.minusSeconds(day), 0, null, CLEANUP_NOW.minusSeconds(31 * day));

        int first = challengeRepository.deleteExpiredOrConsumedBefore(threshold, CLEANUP_NOW);
        int second = challengeRepository.deleteExpiredOrConsumedBefore(threshold, CLEANUP_NOW);

        assertEquals(1, first);
        assertEquals(0, second);
    }

    @Test
    @DisplayName("Cleanup handles multiple users correctly")
    void cleanupAffectsMultipleUsersCorrectly() {
        Role otherRole = roleRepository.findByName("ADMIN")
                .orElseGet(() -> roleRepository.save(Role.create("ADMIN", "Admin role", true, Set.of())));
        User other = userRepository.save(User.create(
                "other_cleanup_user", "hash", "Other Cleanup", "other_cleanup@test.com", null, otherRole.getId()));

        Instant threshold = CLEANUP_NOW.minus(Duration.ofDays(30));
        long day = 86400L;

        saveChallenge(testUser.getId(), "expired_old_a", CLEANUP_NOW.minusSeconds(day), 0, null, CLEANUP_NOW.minusSeconds(31 * day));
        saveChallenge(other.getId(), "expired_old_b", CLEANUP_NOW.minusSeconds(day), 0, null, CLEANUP_NOW.minusSeconds(31 * day));
        TwoFactorChallenge activeA = saveChallenge(testUser.getId(), "active_a", CLEANUP_NOW.plusSeconds(3600), 0, null, CLEANUP_NOW);
        TwoFactorChallenge activeB = saveChallenge(other.getId(), "active_b", CLEANUP_NOW.plusSeconds(3600), 0, null, CLEANUP_NOW);

        int deleted = challengeRepository.deleteExpiredOrConsumedBefore(threshold, CLEANUP_NOW);

        assertEquals(2, deleted);
        assertTrue(challengeRepository.findById(activeA.getId()).isPresent());
        assertTrue(challengeRepository.findById(activeB.getId()).isPresent());
    }
}

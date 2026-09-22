package com.benhsoan.persistence.adapterRepository.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.Set;

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
}

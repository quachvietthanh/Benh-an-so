package com.benhsoan.persistence.jpaRepository.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import com.benhsoan.persistence.entity.auth.UserSessionEntity;

@DataJpaTest(properties = {
        "spring.flyway.enabled=false",
        "spring.sql.init.mode=never",
        "spring.datasource.url=jdbc:h2:mem:user-session-test;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
class UserSessionRepositoryJpaIntegrationTest {

    private static final Instant NOW = Instant.parse("2026-09-24T08:00:00Z");
    private static final UUID USER_ID = UUID.randomUUID();

    @Autowired
    private JpaUserSessionRepository jpaRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void revokeByIdRevokesOnlyOpenSession() {
        UserSessionEntity session = entity(USER_ID, NOW);
        entityManager.persistAndFlush(session);

        int updated = jpaRepository.revokeById(session.getId(), NOW);
        assertEquals(1, updated);

        entityManager.clear();
        UserSessionEntity reloaded = jpaRepository.findById(session.getId()).orElseThrow();
        assertEquals(NOW, reloaded.getRevokedAt());
    }

    @Test
    void revokeByIdReturnsZeroWhenAlreadyRevoked() {
        UserSessionEntity session = entity(USER_ID, NOW);
        session.setRevokedAt(NOW);
        entityManager.persistAndFlush(session);

        int updated = jpaRepository.revokeById(session.getId(), NOW.plusSeconds(1));
        assertEquals(0, updated);
    }

    @Test
    void touchLastUsedUpdatesLastActivity() {
        UserSessionEntity session = entity(USER_ID, NOW);
        entityManager.persistAndFlush(session);

        Instant later = NOW.plusSeconds(500);
        jpaRepository.touchLastUsed(session.getId(), later);

        entityManager.clear();
        UserSessionEntity reloaded = jpaRepository.findById(session.getId()).orElseThrow();
        assertEquals(later, reloaded.getLastUsedAt());
    }

    @Test
    void touchLastUsedDoesNotResurrectRevokedSession() {
        UserSessionEntity session = entity(USER_ID, NOW);
        session.setRevokedAt(NOW);
        entityManager.persistAndFlush(session);

        jpaRepository.touchLastUsed(session.getId(), NOW.plusSeconds(500));

        entityManager.clear();
        UserSessionEntity reloaded = jpaRepository.findById(session.getId()).orElseThrow();
        assertEquals(NOW, reloaded.getRevokedAt());
        assertEquals(NOW, reloaded.getLastUsedAt());
    }

    @Test
    void findAllByRevokedAtIsNullReturnsOnlyOpenSessions() {
        UserSessionEntity open = entity(USER_ID, NOW);
        UserSessionEntity revoked = entity(USER_ID, NOW);
        revoked.setRevokedAt(NOW);
        entityManager.persistAndFlush(open);
        entityManager.persistAndFlush(revoked);

        Page<UserSessionEntity> page = jpaRepository.findAllByRevokedAtIsNull(PageRequest.of(0, 20));

        assertEquals(1, page.getTotalElements());
        assertEquals(open.getId(), page.getContent().get(0).getId());
        assertNull(page.getContent().get(0).getRevokedAt());
    }

    private UserSessionEntity entity(UUID userId, Instant now) {
        return UserSessionEntity.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .refreshTokenHash("hash-" + UUID.randomUUID())
                .refreshExpiresAt(now.plus(Duration.ofDays(7)))
                .createdAt(now)
                .lastUsedAt(now)
                .build();
    }
}
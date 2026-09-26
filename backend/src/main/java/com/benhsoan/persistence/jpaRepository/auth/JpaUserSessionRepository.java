package com.benhsoan.persistence.jpaRepository.auth;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.benhsoan.persistence.entity.auth.UserSessionEntity;

public interface JpaUserSessionRepository extends JpaRepository<UserSessionEntity, UUID> {

    Optional<UserSessionEntity> findByRefreshTokenHash(String refreshTokenHash);

    Optional<UserSessionEntity> findByPreviousRefreshTokenHash(String previousRefreshTokenHash);

    Optional<UserSessionEntity> findFirstByUserIdOrderByCreatedAtDesc(UUID userId);

    default Optional<UserSessionEntity> findByUserId(UUID userId) {
        return findFirstByUserIdOrderByCreatedAtDesc(userId);
    }

    boolean existsByRefreshTokenHash(String refreshTokenHash);

    @Query("""
            SELECT s FROM UserSessionEntity s
            WHERE s.revokedAt IS NULL
              AND s.refreshExpiresAt > :now
              AND COALESCE(s.lastUsedAt, s.createdAt) > :activeThreshold
            ORDER BY COALESCE(s.lastUsedAt, s.createdAt) DESC
            """)
    Page<UserSessionEntity> findActiveSessions(
            @Param("now") Instant now,
            @Param("activeThreshold") Instant activeThreshold,
            Pageable pageable
    );

    @Modifying
    @Query("""
            UPDATE UserSessionEntity s
            SET s.lastUsedAt = :now
            WHERE s.id = :sessionId
            """)
    void touchLastUsed(@Param("sessionId") UUID sessionId, @Param("now") Instant now);

    @Modifying
    @Query("""
            update UserSessionEntity session
            set session.revokedAt = :revokedAt
            where session.userId = :userId and session.revokedAt is null
            """)
    void revokeByUserId(@Param("userId") UUID userId, @Param("revokedAt") Instant revokedAt);

    void deleteByRefreshExpiresAtBefore(Instant time);
}

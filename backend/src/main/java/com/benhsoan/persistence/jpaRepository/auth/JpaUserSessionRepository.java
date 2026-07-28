package com.benhsoan.persistence.jpaRepository.auth;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.benhsoan.persistence.entity.auth.UserSessionEntity;

public interface JpaUserSessionRepository extends JpaRepository<UserSessionEntity, UUID> {

    Optional<UserSessionEntity> findByRefreshTokenHash(String refreshTokenHash);

    Optional<UserSessionEntity> findByPreviousRefreshTokenHash(String previousRefreshTokenHash);

    Optional<UserSessionEntity> findByUserId(UUID userId);

    boolean existsByRefreshTokenHash(String refreshTokenHash);

    @Modifying
    @Query("""
            update UserSessionEntity session
            set session.revokedAt = :revokedAt
            where session.userId = :userId and session.revokedAt is null
            """)
    void revokeByUserId(@Param("userId") UUID userId, @Param("revokedAt") Instant revokedAt);

    void deleteByRefreshExpiresAtBefore(Instant time);
}

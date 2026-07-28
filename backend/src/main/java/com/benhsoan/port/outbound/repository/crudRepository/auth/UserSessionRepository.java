package com.benhsoan.port.outbound.repository.crudRepository.auth;

import java.util.Optional;
import java.util.UUID;
import java.time.Instant;

import com.benhsoan.domain.auth.UserSession;
import com.benhsoan.port.outbound.repository.BaseRepository;

public interface UserSessionRepository extends BaseRepository<UserSession, UUID> {

    Optional<UserSession> findByUserId(UUID userId);

    Optional<UserSession> findByRefreshTokenHash(String refreshTokenHash);

    Optional<UserSession> findByPreviousRefreshTokenHash(String previousRefreshTokenHash);

    boolean existsByRefreshTokenHash(String refreshTokenHash);
    
    void deleteExpiredSessions();

    void revokeByUserId(UUID userId, Instant revokedAt);
}

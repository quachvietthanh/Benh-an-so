package com.benhsoan.port.outbound.repository.auth;

import java.util.Optional;
import java.util.UUID;
import java.time.Instant;

import com.benhsoan.domain.auth.UserSession;
public interface UserSessionRepository {

    Optional<UserSession> findById(UUID id);

    UserSession save(UserSession session);

    Optional<UserSession> findByUserId(UUID userId);

    Optional<UserSession> findByRefreshTokenHash(String refreshTokenHash);

    Optional<UserSession> findByPreviousRefreshTokenHash(String previousRefreshTokenHash);

    boolean existsByRefreshTokenHash(String refreshTokenHash);
    
    void deleteExpiredSessions();

    void revokeByUserId(UUID userId, Instant revokedAt);
}

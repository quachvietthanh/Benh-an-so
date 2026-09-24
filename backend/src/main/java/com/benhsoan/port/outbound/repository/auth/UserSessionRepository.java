package com.benhsoan.port.outbound.repository.auth;

import java.util.Optional;
import java.util.UUID;
import java.time.Instant;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

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

    void touchLastUsed(UUID sessionId, Instant lastUsedAt);

    boolean revokeById(UUID sessionId, Instant revokedAt);

    Page<UserSession> findAllByRevokedAtIsNull(Pageable pageable);
}

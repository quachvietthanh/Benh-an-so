package com.benhsoan.port.outbound.repository.auth;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

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

    Page<UserSession> findActiveSessions(Instant now, Instant activeThreshold, Pageable pageable);

    default Page<UserSession> findActiveSessions(Instant activeThreshold, Pageable pageable) {
        return findActiveSessions(Instant.now(), activeThreshold, pageable);
    }

    void touchLastUsed(UUID sessionId, Instant lastUsedAt);

    void deleteExpiredSessions();

    void revokeByUserId(UUID userId, Instant revokedAt);
}

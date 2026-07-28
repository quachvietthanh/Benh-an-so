package com.benhsoan.persistence.adapterRepository.auth;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.benhsoan.domain.auth.UserSession;
import com.benhsoan.persistence.entity.auth.UserSessionEntity;
import com.benhsoan.persistence.jpaRepository.auth.JpaUserSessionRepository;
import com.benhsoan.persistence.mapper.auth.UserSessionPersistenceMapper;
import com.benhsoan.port.outbound.repository.crudRepository.auth.UserSessionRepository;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class UserSessionRepositoryAdapter implements UserSessionRepository {

    private final JpaUserSessionRepository jpaRepository;

    private final UserSessionPersistenceMapper mapper;

    @Override
    public Optional<UserSession> findById(UUID id) {
        return jpaRepository.findById(id)
                .map(mapper::toDomain);
    }

    @Override
    public Optional<UserSession> findByRefreshTokenHash(String refreshTokenHash) {
        return jpaRepository.findByRefreshTokenHash(refreshTokenHash)
                .map(mapper::toDomain);
    }

    @Override
    public Optional<UserSession> findByPreviousRefreshTokenHash(String previousRefreshTokenHash) {
        return jpaRepository.findByPreviousRefreshTokenHash(previousRefreshTokenHash)
                .map(mapper::toDomain);
    }

    @Override
    public Optional<UserSession> findByUserId(UUID userId) {
        return jpaRepository.findByUserId(userId)
                .map(mapper::toDomain);
    }

    @Override
    public UserSession save(UserSession session) {

        UserSessionEntity entity = mapper.toEntity(session);

        UserSessionEntity saved = jpaRepository.save(entity);

        return mapper.toDomain(saved);
    }

    @Override
    public void deleteById(UUID id) {
        if(id == null) return;
        jpaRepository.deleteById(id);
    }

    @Override
    public boolean existsByRefreshTokenHash(String refreshTokenHash) {
        return jpaRepository.existsByRefreshTokenHash(refreshTokenHash);
    }

    @Override
    public void revokeByUserId(UUID userId, Instant revokedAt) {
        jpaRepository.revokeByUserId(userId, revokedAt);
    }

    @Override
    public void deleteExpiredSessions() {
        jpaRepository.deleteByRefreshExpiresAtBefore(Instant.now());
    }
}

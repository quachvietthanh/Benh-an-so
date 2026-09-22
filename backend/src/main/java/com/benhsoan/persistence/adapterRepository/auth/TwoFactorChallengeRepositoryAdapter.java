package com.benhsoan.persistence.adapterRepository.auth;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.benhsoan.domain.auth.TwoFactorChallenge;
import com.benhsoan.persistence.entity.auth.TwoFactorChallengeEntity;
import com.benhsoan.persistence.jpaRepository.auth.JpaTwoFactorChallengeRepository;
import com.benhsoan.persistence.mapper.auth.TwoFactorChallengePersistenceMapper;
import com.benhsoan.port.outbound.repository.auth.TwoFactorChallengeRepository;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class TwoFactorChallengeRepositoryAdapter implements TwoFactorChallengeRepository {

    private final JpaTwoFactorChallengeRepository jpaRepository;
    private final TwoFactorChallengePersistenceMapper mapper;

    @Override
    public TwoFactorChallenge save(TwoFactorChallenge challenge) {
        TwoFactorChallengeEntity saved = jpaRepository.save(mapper.toEntity(challenge));
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<TwoFactorChallenge> findById(UUID id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public int incrementAttempts(UUID id, int maxAttempts) {
        return jpaRepository.incrementAttempts(id, maxAttempts);
    }

    @Override
    public int markConsumed(UUID id, Instant consumedAt) {
        return jpaRepository.markConsumed(id, consumedAt);
    }

    @Override
    public void invalidatePendingChallengesByUserId(UUID userId, Instant invalidatedAt) {
        jpaRepository.invalidatePendingChallengesByUserId(userId, invalidatedAt);
    }
}

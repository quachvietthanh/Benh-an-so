package com.benhsoan.persistence.adapterRepository.auth;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.benhsoan.domain.auth.PatientPasswordRecoveryToken;
import com.benhsoan.persistence.entity.auth.PatientPasswordRecoveryTokenEntity;
import com.benhsoan.persistence.jpaRepository.auth.JpaPatientPasswordRecoveryTokenRepository;
import com.benhsoan.persistence.mapper.auth.PatientPasswordRecoveryTokenPersistenceMapper;
import com.benhsoan.port.outbound.repository.auth.PatientPasswordRecoveryTokenRepository;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class PatientPasswordRecoveryTokenRepositoryAdapter implements PatientPasswordRecoveryTokenRepository {

    private final JpaPatientPasswordRecoveryTokenRepository jpaRepository;
    private final PatientPasswordRecoveryTokenPersistenceMapper mapper;

    @Override
    public PatientPasswordRecoveryToken save(PatientPasswordRecoveryToken token) {
        PatientPasswordRecoveryTokenEntity entity = mapper.toEntity(token);
        PatientPasswordRecoveryTokenEntity saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<PatientPasswordRecoveryToken> findLatestByPhone(String phone) {
        return jpaRepository.findTopByPhoneOrderByCreatedAtDesc(phone)
                .map(mapper::toDomain);
    }

    @Override
    public Optional<PatientPasswordRecoveryToken> findLatestActiveByPhone(String phone) {
        return jpaRepository.findTopByPhoneAndUsedAtIsNullOrderByCreatedAtDesc(phone)
                .map(mapper::toDomain);
    }

    @Override
    public Optional<PatientPasswordRecoveryToken> findLatestActiveByUserId(UUID userId) {
        return jpaRepository.findTopByUserIdAndUsedAtIsNullOrderByCreatedAtDesc(userId)
                .map(mapper::toDomain);
    }

    @Override
    public void invalidateActiveTokensByPhone(String phone, Instant invalidatedAt) {
        jpaRepository.invalidateActiveTokensByPhone(phone, invalidatedAt);
    }

    @Override
    public int incrementAttempts(UUID tokenId) {
        return jpaRepository.incrementAttemptsById(tokenId);
    }

    @Override
    public int getAttempts(UUID tokenId) {
        Integer attempts = jpaRepository.findAttemptsById(tokenId);
        return attempts != null ? attempts : 0;
    }
}

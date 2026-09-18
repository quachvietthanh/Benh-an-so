package com.benhsoan.port.outbound.repository.auth;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import com.benhsoan.domain.auth.PatientPasswordRecoveryToken;

public interface PatientPasswordRecoveryTokenRepository {

    PatientPasswordRecoveryToken save(PatientPasswordRecoveryToken token);

    Optional<PatientPasswordRecoveryToken> findLatestByPhone(String phone);

    Optional<PatientPasswordRecoveryToken> findLatestActiveByPhone(String phone);

    Optional<PatientPasswordRecoveryToken> findLatestActiveByUserId(UUID userId);

    void invalidateActiveTokensByPhone(String phone, Instant invalidatedAt);

    int incrementAttempts(UUID tokenId);

    int getAttempts(UUID tokenId);
}

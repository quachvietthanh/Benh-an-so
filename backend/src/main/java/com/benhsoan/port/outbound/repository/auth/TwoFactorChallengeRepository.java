package com.benhsoan.port.outbound.repository.auth;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import com.benhsoan.domain.auth.TwoFactorChallenge;

public interface TwoFactorChallengeRepository {

    TwoFactorChallenge save(TwoFactorChallenge challenge);

    Optional<TwoFactorChallenge> findById(UUID id);

    int incrementAttempts(UUID id);

    int markConsumed(UUID id, Instant consumedAt);
}

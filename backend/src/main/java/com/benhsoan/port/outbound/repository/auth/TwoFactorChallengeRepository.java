package com.benhsoan.port.outbound.repository.auth;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import com.benhsoan.domain.auth.TwoFactorChallenge;

public interface TwoFactorChallengeRepository {

    TwoFactorChallenge save(TwoFactorChallenge challenge);

    Optional<TwoFactorChallenge> findById(UUID id);

    /**
     * Atomically increments the failed-attempt counter only while it remains below
     * {@code maxAttempts}, preventing the brute-force limit from being bypassed by
     * concurrent verification requests. Returns the number of updated rows (0 when
     * the limit has already been reached or the challenge no longer exists).
     */
    int incrementAttempts(UUID id, int maxAttempts);

    int markConsumed(UUID id, Instant consumedAt);

    /**
     * Marks all still-pending challenges for the given user as consumed so only the
     * most recently issued challenge remains usable.
     */
    void invalidatePendingChallengesByUserId(UUID userId, Instant invalidatedAt);

    /**
     * Deletes challenges that are no longer usable (already consumed or already
     * expired) and were created before the retention threshold. Never deletes an
     * active challenge. Returns the number of deleted rows.
     */
    int deleteExpiredOrConsumedBefore(Instant retentionThreshold, Instant now);
}

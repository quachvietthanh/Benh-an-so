package com.benhsoan.port.inbound.auth;

/**
 * Purges two-factor challenge records that are no longer usable (consumed or
 * expired) and have passed the retention window. Never removes an active
 * challenge.
 */
public interface TwoFactorChallengeCleanupUseCase {

    /**
     * @return the number of challenge records deleted.
     */
    int cleanupExpiredChallenges();
}
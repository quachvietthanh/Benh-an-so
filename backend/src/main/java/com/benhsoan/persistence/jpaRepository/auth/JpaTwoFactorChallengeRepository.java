package com.benhsoan.persistence.jpaRepository.auth;

import java.time.Instant;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.benhsoan.persistence.entity.auth.TwoFactorChallengeEntity;

public interface JpaTwoFactorChallengeRepository extends JpaRepository<TwoFactorChallengeEntity, UUID> {

    @Modifying
    @Query("UPDATE TwoFactorChallengeEntity c SET c.attempts = c.attempts + 1 WHERE c.id = :id AND c.attempts < :maxAttempts")
    int incrementAttempts(@Param("id") UUID id, @Param("maxAttempts") int maxAttempts);

    @Modifying
    @Query("UPDATE TwoFactorChallengeEntity c SET c.consumedAt = :consumedAt WHERE c.id = :id AND c.consumedAt IS NULL")
    int markConsumed(@Param("id") UUID id, @Param("consumedAt") Instant consumedAt);

    @Modifying
    @Query("UPDATE TwoFactorChallengeEntity c SET c.consumedAt = :invalidatedAt WHERE c.userId = :userId AND c.consumedAt IS NULL")
    int invalidatePendingChallengesByUserId(@Param("userId") UUID userId, @Param("invalidatedAt") Instant invalidatedAt);
}

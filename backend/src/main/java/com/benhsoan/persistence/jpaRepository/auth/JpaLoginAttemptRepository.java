package com.benhsoan.persistence.jpaRepository.auth;

import java.time.Instant;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.benhsoan.persistence.entity.auth.LoginAttemptEntity;

public interface JpaLoginAttemptRepository extends JpaRepository<LoginAttemptEntity, String> {

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        UPDATE LoginAttemptEntity e
        SET e.attempts = CASE
                WHEN e.blockedUntil IS NOT NULL AND e.blockedUntil <= :now THEN 1
                ELSE e.attempts + 1
            END,
            e.blockedUntil = CASE
                WHEN e.blockedUntil IS NOT NULL AND e.blockedUntil > :now THEN e.blockedUntil
                WHEN (CASE WHEN e.blockedUntil IS NOT NULL AND e.blockedUntil <= :now THEN 1 ELSE e.attempts + 1 END) >= :maxAttempts
                    THEN :newBlockedUntil
                ELSE NULL
            END,
            e.updatedAt = :now
        WHERE e.identifier = :identifier
    """)
    int atomicIncrement(
            @Param("identifier") String identifier,
            @Param("now") Instant now,
            @Param("maxAttempts") int maxAttempts,
            @Param("newBlockedUntil") Instant newBlockedUntil
    );
}

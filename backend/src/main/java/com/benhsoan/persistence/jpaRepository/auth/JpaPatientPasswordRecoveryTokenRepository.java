package com.benhsoan.persistence.jpaRepository.auth;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import com.benhsoan.persistence.entity.auth.PatientPasswordRecoveryTokenEntity;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface JpaPatientPasswordRecoveryTokenRepository
        extends JpaRepository<PatientPasswordRecoveryTokenEntity, UUID> {

    Optional<PatientPasswordRecoveryTokenEntity> findTopByPhoneOrderByCreatedAtDesc(String phone);

    Optional<PatientPasswordRecoveryTokenEntity> findTopByPhoneAndUsedAtIsNullOrderByCreatedAtDesc(String phone);

    Optional<PatientPasswordRecoveryTokenEntity> findTopByUserIdAndUsedAtIsNullOrderByCreatedAtDesc(UUID userId);

    @Modifying
    @Query("UPDATE PatientPasswordRecoveryTokenEntity t SET t.attempts = t.attempts + 1 WHERE t.id = :id")
    int incrementAttemptsById(@Param("id") UUID id);

    @Query("SELECT t.attempts FROM PatientPasswordRecoveryTokenEntity t WHERE t.id = :id")
    Integer findAttemptsById(@Param("id") UUID id);
}

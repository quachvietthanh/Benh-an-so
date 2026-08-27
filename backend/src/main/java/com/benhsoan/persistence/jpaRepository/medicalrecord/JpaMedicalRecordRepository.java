package com.benhsoan.persistence.jpaRepository.medicalrecord;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.benhsoan.persistence.entity.medicalrecord.MedicalRecordEntity;

import jakarta.persistence.LockModeType;

public interface JpaMedicalRecordRepository extends JpaRepository<MedicalRecordEntity, UUID> {

    Optional<MedicalRecordEntity> findByVisitId(UUID visitId);

    List<MedicalRecordEntity> findByVisitIdIn(Collection<UUID> visitIds);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select medicalRecord from MedicalRecordEntity medicalRecord where medicalRecord.id = :id")
    Optional<MedicalRecordEntity> findByIdForUpdate(@Param("id") UUID id);

    boolean existsByVisitId(UUID visitId);
}

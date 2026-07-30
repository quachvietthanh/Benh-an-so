package com.benhsoan.persistence.jpaRepository.medicalrecord;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.benhsoan.persistence.entity.medicalrecord.MedicalRecordEntity;

public interface JpaMedicalRecordRepository extends JpaRepository<MedicalRecordEntity, UUID> {

    Optional<MedicalRecordEntity> findByVisitId(UUID visitId);

    boolean existsByVisitId(UUID visitId);
}

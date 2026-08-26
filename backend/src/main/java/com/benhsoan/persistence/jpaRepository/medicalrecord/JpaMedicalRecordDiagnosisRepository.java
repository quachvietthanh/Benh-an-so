package com.benhsoan.persistence.jpaRepository.medicalrecord;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;

import com.benhsoan.persistence.entity.medicalrecord.MedicalRecordDiagnosisEntity;

public interface JpaMedicalRecordDiagnosisRepository extends JpaRepository<MedicalRecordDiagnosisEntity, UUID> {

    List<MedicalRecordDiagnosisEntity> findByMedicalRecordId(UUID medicalRecordId);

    boolean existsByMedicalRecordId(UUID medicalRecordId);

    boolean existsByDiagnosisCatalogId(UUID diagnosisCatalogId);

    @Modifying
    void deleteByMedicalRecordId(UUID medicalRecordId);
}

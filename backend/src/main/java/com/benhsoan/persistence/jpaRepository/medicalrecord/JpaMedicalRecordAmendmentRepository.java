package com.benhsoan.persistence.jpaRepository.medicalrecord;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.benhsoan.persistence.entity.medicalrecord.MedicalRecordAmendmentEntity;

public interface JpaMedicalRecordAmendmentRepository
        extends JpaRepository<MedicalRecordAmendmentEntity, UUID> {

    List<MedicalRecordAmendmentEntity> findByMedicalRecordIdOrderByAmendedAtDesc(UUID medicalRecordId);

    @Modifying
    @Query("delete from MedicalRecordAmendmentEntity amendment where amendment.medicalRecordId = :medicalRecordId")
    void deleteByMedicalRecordId(@Param("medicalRecordId") UUID medicalRecordId);
}

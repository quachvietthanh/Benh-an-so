package com.benhsoan.persistence.jpaRepository.medicalrecord;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.benhsoan.persistence.entity.medicalrecord.MedicalRecordAmendmentEntity;

public interface JpaMedicalRecordAmendmentRepository
        extends JpaRepository<MedicalRecordAmendmentEntity, UUID> {

    List<MedicalRecordAmendmentEntity> findByMedicalRecordIdOrderByAmendedAtDesc(UUID medicalRecordId);
}

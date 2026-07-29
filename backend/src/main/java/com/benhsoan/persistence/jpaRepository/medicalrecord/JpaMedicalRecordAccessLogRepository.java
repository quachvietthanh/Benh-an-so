package com.benhsoan.persistence.jpaRepository.medicalrecord;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.benhsoan.persistence.entity.medicalrecord.MedicalRecordAccessLogEntity;

public interface JpaMedicalRecordAccessLogRepository
        extends JpaRepository<MedicalRecordAccessLogEntity, UUID> {
}

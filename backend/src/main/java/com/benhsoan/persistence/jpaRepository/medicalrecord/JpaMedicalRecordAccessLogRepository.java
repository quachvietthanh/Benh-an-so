package com.benhsoan.persistence.jpaRepository.medicalrecord;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.benhsoan.persistence.entity.medicalrecord.MedicalRecordAccessLogEntity;

public interface JpaMedicalRecordAccessLogRepository
        extends JpaRepository<MedicalRecordAccessLogEntity, UUID>,
        JpaSpecificationExecutor<MedicalRecordAccessLogEntity> {
}

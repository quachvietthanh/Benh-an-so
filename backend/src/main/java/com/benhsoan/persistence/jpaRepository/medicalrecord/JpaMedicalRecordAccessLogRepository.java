package com.benhsoan.persistence.jpaRepository.medicalrecord;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.benhsoan.persistence.entity.medicalrecord.MedicalRecordAccessLogEntity;

public interface JpaMedicalRecordAccessLogRepository
        extends JpaRepository<MedicalRecordAccessLogEntity, UUID>,
        JpaSpecificationExecutor<MedicalRecordAccessLogEntity> {

    @Modifying
    @Query("delete from MedicalRecordAccessLogEntity log where log.medicalRecordId = :medicalRecordId")
    void deleteByMedicalRecordId(@Param("medicalRecordId") UUID medicalRecordId);
}

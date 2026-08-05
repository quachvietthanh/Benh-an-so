package com.benhsoan.persistence.jpaRepository.prescription;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.benhsoan.persistence.entity.prescription.PrescriptionWarningLogEntity;

public interface JpaPrescriptionWarningLogRepository
        extends JpaRepository<PrescriptionWarningLogEntity, UUID> {

    List<PrescriptionWarningLogEntity> findByPrescriptionIdOrderByCreatedAtAsc(
            UUID prescriptionId
    );
}

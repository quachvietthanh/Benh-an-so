package com.benhsoan.persistence.jpaRepository.prescription;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.benhsoan.persistence.entity.prescription.PrescriptionContraindicationWarningLogEntity;

public interface JpaPrescriptionContraindicationWarningLogRepository
        extends JpaRepository<PrescriptionContraindicationWarningLogEntity, UUID> {

    List<PrescriptionContraindicationWarningLogEntity> findByPrescriptionIdOrderByHandledAtAsc(UUID prescriptionId);
}

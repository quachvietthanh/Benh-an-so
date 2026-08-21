package com.benhsoan.persistence.jpaRepository.prescription;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.benhsoan.persistence.entity.prescription.PrescriptionInterconnectionLogEntity;

public interface JpaPrescriptionInterconnectionLogRepository
        extends JpaRepository<PrescriptionInterconnectionLogEntity, UUID> {

    List<PrescriptionInterconnectionLogEntity> findByPrescriptionIdOrderByAttemptNumberAsc(UUID prescriptionId);
}

package com.benhsoan.persistence.jpaRepository.prescription;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.benhsoan.persistence.entity.prescription.PrescriptionAmendmentEntity;

public interface JpaPrescriptionAmendmentRepository
        extends JpaRepository<PrescriptionAmendmentEntity, UUID> {

    List<PrescriptionAmendmentEntity> findByPrescriptionIdOrderByAmendedAtAsc(
            UUID prescriptionId
    );
}

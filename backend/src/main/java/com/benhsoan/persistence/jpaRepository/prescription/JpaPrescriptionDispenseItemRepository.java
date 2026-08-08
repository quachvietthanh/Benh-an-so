package com.benhsoan.persistence.jpaRepository.prescription;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.benhsoan.persistence.entity.prescription.PrescriptionDispenseItemEntity;

public interface JpaPrescriptionDispenseItemRepository
        extends JpaRepository<PrescriptionDispenseItemEntity, UUID> {

    List<PrescriptionDispenseItemEntity> findByPrescriptionIdOrderByDispensedAtAsc(UUID prescriptionId);

    List<PrescriptionDispenseItemEntity> findByPrescriptionItemIdOrderByDispensedAtAsc(UUID prescriptionItemId);
}

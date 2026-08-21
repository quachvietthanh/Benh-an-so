package com.benhsoan.persistence.jpaRepository.prescription;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.benhsoan.persistence.entity.prescription.PrescriptionDispenseItemEntity;

public interface JpaPrescriptionDispenseItemRepository
        extends JpaRepository<PrescriptionDispenseItemEntity, UUID> {

    List<PrescriptionDispenseItemEntity> findByPrescriptionIdOrderByDispensedAtAsc(UUID prescriptionId);

    List<PrescriptionDispenseItemEntity> findByPrescriptionItemIdOrderByDispensedAtAsc(UUID prescriptionItemId);

    @Modifying
    @Query("delete from PrescriptionDispenseItemEntity dispense where dispense.prescriptionId in :prescriptionIds")
    void deleteByPrescriptionIdIn(@Param("prescriptionIds") Collection<UUID> prescriptionIds);
}

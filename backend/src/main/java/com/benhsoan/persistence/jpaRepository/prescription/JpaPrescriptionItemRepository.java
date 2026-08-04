package com.benhsoan.persistence.jpaRepository.prescription;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.benhsoan.persistence.entity.prescription.PrescriptionItemEntity;

public interface JpaPrescriptionItemRepository
        extends JpaRepository<PrescriptionItemEntity, UUID> {

    List<PrescriptionItemEntity> findByPrescriptionIdOrderByCreatedAtAsc(
            UUID prescriptionId
    );

    @Modifying(flushAutomatically = true)
    @Query("delete from PrescriptionItemEntity item "
            + "where item.prescriptionId = :prescriptionId")
    void deleteAllByPrescriptionId(@Param("prescriptionId") UUID prescriptionId);
}

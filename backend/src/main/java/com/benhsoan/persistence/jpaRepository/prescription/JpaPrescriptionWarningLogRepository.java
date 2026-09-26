package com.benhsoan.persistence.jpaRepository.prescription;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.benhsoan.persistence.entity.prescription.PrescriptionWarningLogEntity;

public interface JpaPrescriptionWarningLogRepository
        extends JpaRepository<PrescriptionWarningLogEntity, UUID> {

    List<PrescriptionWarningLogEntity> findByPrescriptionIdOrderByCreatedAtAsc(
            UUID prescriptionId
    );

    @Modifying
    @Query("delete from PrescriptionWarningLogEntity warning where warning.prescriptionId in :prescriptionIds")
    void deleteByPrescriptionIdIn(@Param("prescriptionIds") Collection<UUID> prescriptionIds);
}

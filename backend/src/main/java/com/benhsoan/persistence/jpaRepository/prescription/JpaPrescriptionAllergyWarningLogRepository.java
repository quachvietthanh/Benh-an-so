package com.benhsoan.persistence.jpaRepository.prescription;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.benhsoan.persistence.entity.prescription.PrescriptionAllergyWarningLogEntity;

public interface JpaPrescriptionAllergyWarningLogRepository
        extends JpaRepository<PrescriptionAllergyWarningLogEntity, UUID>,
        JpaSpecificationExecutor<PrescriptionAllergyWarningLogEntity> {

    List<PrescriptionAllergyWarningLogEntity> findByPrescriptionIdOrderByCreatedAtAsc(
            UUID prescriptionId
    );

    @Modifying
    @Query("delete from PrescriptionAllergyWarningLogEntity warning where warning.prescriptionId in :prescriptionIds")
    void deleteByPrescriptionIdIn(@Param("prescriptionIds") Collection<UUID> prescriptionIds);
}

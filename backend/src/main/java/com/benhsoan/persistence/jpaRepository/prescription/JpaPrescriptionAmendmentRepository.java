package com.benhsoan.persistence.jpaRepository.prescription;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.benhsoan.persistence.entity.prescription.PrescriptionAmendmentEntity;

public interface JpaPrescriptionAmendmentRepository
        extends JpaRepository<PrescriptionAmendmentEntity, UUID> {

    List<PrescriptionAmendmentEntity> findByPrescriptionIdOrderByAmendedAtAsc(
            UUID prescriptionId
    );

    @Modifying
    @Query("delete from PrescriptionAmendmentEntity amendment where amendment.prescriptionId in :prescriptionIds")
    void deleteByPrescriptionIdIn(@Param("prescriptionIds") Collection<UUID> prescriptionIds);
}

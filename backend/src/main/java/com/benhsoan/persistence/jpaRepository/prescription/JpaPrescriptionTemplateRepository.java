package com.benhsoan.persistence.jpaRepository.prescription;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.benhsoan.persistence.entity.prescription.PrescriptionTemplateEntity;

public interface JpaPrescriptionTemplateRepository
        extends JpaRepository<PrescriptionTemplateEntity, UUID> {

    List<PrescriptionTemplateEntity> findByDiagnosisCatalogIdOrderByCreatedAtDesc(UUID diagnosisCatalogId);

    List<PrescriptionTemplateEntity> findByDiagnosisCatalogIdAndCreatedByOrderByCreatedAtDesc(
            UUID diagnosisCatalogId,
            UUID createdBy
    );
}

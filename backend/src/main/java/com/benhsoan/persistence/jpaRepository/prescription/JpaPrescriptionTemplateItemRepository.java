package com.benhsoan.persistence.jpaRepository.prescription;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.benhsoan.persistence.entity.prescription.PrescriptionTemplateItemEntity;

public interface JpaPrescriptionTemplateItemRepository
        extends JpaRepository<PrescriptionTemplateItemEntity, UUID> {

    List<PrescriptionTemplateItemEntity> findByTemplateIdOrderBySortOrderAsc(UUID templateId);

    void deleteAllByTemplateId(UUID templateId);
}

package com.benhsoan.persistence.jpaRepository.medicalrecord;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.benhsoan.persistence.entity.medicalrecord.MedicalRecordTemplateVersionEntity;

public interface JpaMedicalRecordTemplateVersionRepository
        extends JpaRepository<MedicalRecordTemplateVersionEntity, UUID> {

    List<MedicalRecordTemplateVersionEntity> findByTemplateIdOrderByVersionNoAsc(UUID templateId);
}

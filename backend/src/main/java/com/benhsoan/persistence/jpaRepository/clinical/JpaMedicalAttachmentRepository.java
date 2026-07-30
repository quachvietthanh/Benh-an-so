package com.benhsoan.persistence.jpaRepository.clinical;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.benhsoan.persistence.entity.clinical.MedicalAttachmentEntity;

public interface JpaMedicalAttachmentRepository extends JpaRepository<MedicalAttachmentEntity, UUID> {
    List<MedicalAttachmentEntity> findByClinicalResultIdOrderByUploadedAtDesc(UUID clinicalResultId);
    List<MedicalAttachmentEntity> findByClinicalResultIdIn(Collection<UUID> clinicalResultIds);
}

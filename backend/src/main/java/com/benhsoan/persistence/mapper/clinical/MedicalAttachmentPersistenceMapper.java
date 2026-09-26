package com.benhsoan.persistence.mapper.clinical;

import org.springframework.stereotype.Component;

import com.benhsoan.domain.clinical.MedicalAttachment;
import com.benhsoan.persistence.entity.clinical.MedicalAttachmentEntity;

@Component
public class MedicalAttachmentPersistenceMapper {

    public MedicalAttachment toDomain(MedicalAttachmentEntity entity) {
        if (entity == null) {
            return null;
        }

        return MedicalAttachment.restore(
                entity.getId(),
                entity.getVisitId(),
                entity.getMedicalRecordId(),
                entity.getClinicalResultId(),
                entity.getFileName(),
                entity.getOriginalFileName(),
                entity.getStorageKey(),
                entity.getContentType(),
                entity.getFileSize(),
                entity.getChecksum(),
                entity.getAttachmentType(),
                entity.getUploadedBy(),
                entity.getUploadedAt()
        );
    }

    public MedicalAttachmentEntity toEntity(MedicalAttachment domain) {
        if (domain == null) {
            return null;
        }

        return MedicalAttachmentEntity.builder()
                .id(domain.getId())
                .visitId(domain.getVisitId())
                .medicalRecordId(domain.getMedicalRecordId())
                .clinicalResultId(domain.getClinicalResultId())
                .fileName(domain.getFileName())
                .originalFileName(domain.getOriginalFileName())
                .storageKey(domain.getStorageKey())
                .contentType(domain.getContentType())
                .fileSize(domain.getFileSize())
                .checksum(domain.getChecksum())
                .attachmentType(domain.getAttachmentType())
                .uploadedBy(domain.getUploadedBy())
                .uploadedAt(domain.getUploadedAt())
                .build();
    }
}

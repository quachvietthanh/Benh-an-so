package com.benhsoan.persistence.mapper.medicalrecord;

import org.springframework.stereotype.Component;

import com.benhsoan.domain.medicalrecord.MedicalRecord;
import com.benhsoan.persistence.entity.medicalrecord.MedicalRecordEntity;

@Component
public class MedicalRecordPersistenceMapper {

    public MedicalRecord toDomain(MedicalRecordEntity entity) {
        if (entity == null) {
            return null;
        }
        return MedicalRecord.restore(
                entity.getId(),
                entity.getVisitId(),
                entity.getChiefComplaint(),
                entity.getSymptoms(),
                entity.getMedicalHistory(),
                entity.getPhysicalExamination(),
                entity.getClinicalProgress(),
                entity.getTreatmentPlan(),
                entity.getDoctorInstructions(),
                entity.getConclusion(),
                entity.getStatus(),
                entity.getSignatureData(),
                entity.getSignedAt(),
                entity.getSignedBy(),
                entity.getLockedAt(),
                entity.getLockedBy(),
                entity.getCreatedBy(),
                entity.getCreatedAt(),
                entity.getUpdatedBy(),
                entity.getUpdatedAt(),
                entity.getAppliedTemplateVersionId(),
                entity.getTemplateAppliedBy(),
                entity.getTemplateAppliedAt()
        );
    }

    public MedicalRecordEntity toEntity(MedicalRecord domain) {
        if (domain == null) {
            return null;
        }
        return MedicalRecordEntity.builder()
                .id(domain.getId()).visitId(domain.getVisitId()).chiefComplaint(domain.getChiefComplaint())
                .symptoms(domain.getSymptoms()).medicalHistory(domain.getMedicalHistory())
                .physicalExamination(domain.getPhysicalExamination()).clinicalProgress(domain.getClinicalProgress())
                .treatmentPlan(domain.getTreatmentPlan()).doctorInstructions(domain.getDoctorInstructions())
                .conclusion(domain.getConclusion()).status(domain.getStatus()).signatureData(domain.getSignatureData())
                .signedAt(domain.getSignedAt()).signedBy(domain.getSignedBy()).lockedAt(domain.getLockedAt())
                .lockedBy(domain.getLockedBy()).createdBy(domain.getCreatedBy()).createdAt(domain.getCreatedAt())
                .updatedBy(domain.getUpdatedBy()).updatedAt(domain.getUpdatedAt())
                .appliedTemplateVersionId(domain.getAppliedTemplateVersionId())
                .templateAppliedBy(domain.getTemplateAppliedBy()).templateAppliedAt(domain.getTemplateAppliedAt())
                .build();
    }
}

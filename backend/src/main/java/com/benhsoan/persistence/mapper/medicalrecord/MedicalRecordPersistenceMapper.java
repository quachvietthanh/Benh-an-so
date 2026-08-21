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
                entity.getUpdatedAt()
        );
    }

    public MedicalRecordEntity toEntity(MedicalRecord domain) {
        if (domain == null) {
            return null;
        }
        return new MedicalRecordEntity(
                domain.getId(),
                domain.getVisitId(),
                domain.getChiefComplaint(),
                domain.getSymptoms(),
                domain.getMedicalHistory(),
                domain.getPhysicalExamination(),
                domain.getClinicalProgress(),
                domain.getTreatmentPlan(),
                domain.getDoctorInstructions(),
                domain.getConclusion(),
                domain.getStatus(),
                domain.getSignatureData(),
                domain.getSignedAt(),
                domain.getSignedBy(),
                domain.getLockedAt(),
                domain.getLockedBy(),
                domain.getCreatedBy(),
                domain.getCreatedAt(),
                domain.getUpdatedBy(),
                domain.getUpdatedAt()
        );
    }
}

package com.benhsoan.persistence.mapper.prescription;

import org.springframework.stereotype.Component;

import com.benhsoan.domain.prescription.PrescriptionContraindicationWarningLog;
import com.benhsoan.persistence.entity.prescription.PrescriptionContraindicationWarningLogEntity;

@Component
public class PrescriptionContraindicationWarningLogPersistenceMapper {

    public PrescriptionContraindicationWarningLog toDomain(PrescriptionContraindicationWarningLogEntity entity) {
        if (entity == null) {
            return null;
        }
        return PrescriptionContraindicationWarningLog.restore(
                entity.getId(),
                entity.getPrescriptionId(),
                entity.getPatientId(),
                entity.getRuleId(),
                entity.getMedicineId(),
                entity.getContraindicationType(),
                entity.getSeverity(),
                entity.getMessage(),
                entity.getRecommendation(),
                entity.getOverrideReason(),
                entity.getHandledBy(),
                entity.getHandledAt(),
                entity.getCreatedAt()
        );
    }

    public PrescriptionContraindicationWarningLogEntity toEntity(PrescriptionContraindicationWarningLog domain) {
        if (domain == null) {
            return null;
        }
        return PrescriptionContraindicationWarningLogEntity.builder()
                .id(domain.getId())
                .prescriptionId(domain.getPrescriptionId())
                .patientId(domain.getPatientId())
                .ruleId(domain.getRuleId())
                .medicineId(domain.getMedicineId())
                .contraindicationType(domain.getContraindicationType())
                .severity(domain.getSeverity())
                .message(domain.getMessage())
                .recommendation(domain.getRecommendation())
                .overrideReason(domain.getOverrideReason())
                .handledBy(domain.getHandledBy())
                .handledAt(domain.getHandledAt())
                .createdAt(domain.getCreatedAt())
                .build();
    }
}

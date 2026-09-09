package com.benhsoan.persistence.mapper.prescription;

import org.springframework.stereotype.Component;

import com.benhsoan.domain.prescription.PrescriptionAllergyWarningLog;
import com.benhsoan.persistence.entity.prescription.PrescriptionAllergyWarningLogEntity;

@Component
public class PrescriptionAllergyWarningLogPersistenceMapper {

    public PrescriptionAllergyWarningLog toDomain(PrescriptionAllergyWarningLogEntity entity) {
        if (entity == null) {
            return null;
        }

        return PrescriptionAllergyWarningLog.restore(
                entity.getId(),
                entity.getPrescriptionId(),
                entity.getPatientId(),
                entity.getAllergyId(),
                entity.getMedicineId(),
                entity.getActiveIngredient(),
                entity.getAllergenName(),
                entity.getSeverity(),
                entity.getReaction(),
                entity.getOverrideReason(),
                entity.getHandledBy(),
                entity.getHandledAt(),
                entity.getCreatedAt()
        );
    }

    public PrescriptionAllergyWarningLogEntity toEntity(PrescriptionAllergyWarningLog domain) {
        if (domain == null) {
            return null;
        }

        return PrescriptionAllergyWarningLogEntity.builder()
                .id(domain.getId())
                .prescriptionId(domain.getPrescriptionId())
                .patientId(domain.getPatientId())
                .allergyId(domain.getAllergyId())
                .medicineId(domain.getMedicineId())
                .activeIngredient(domain.getActiveIngredient())
                .allergenName(domain.getAllergenName())
                .severity(domain.getSeverity())
                .reaction(domain.getReaction())
                .overrideReason(domain.getOverrideReason())
                .handledBy(domain.getHandledBy())
                .handledAt(domain.getHandledAt())
                .createdAt(domain.getCreatedAt())
                .build();
    }
}

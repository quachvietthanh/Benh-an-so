package com.benhsoan.persistence.mapper.prescription;

import org.springframework.stereotype.Component;

import com.benhsoan.domain.prescription.PrescriptionMaxDailyDoseWarningLog;
import com.benhsoan.persistence.entity.prescription.PrescriptionMaxDailyDoseWarningLogEntity;

@Component
public class PrescriptionMaxDailyDoseWarningLogPersistenceMapper {

    public PrescriptionMaxDailyDoseWarningLog toDomain(PrescriptionMaxDailyDoseWarningLogEntity entity) {
        if (entity == null) {
            return null;
        }
        return PrescriptionMaxDailyDoseWarningLog.restore(
                entity.getId(),
                entity.getPrescriptionId(),
                entity.getPatientId(),
                entity.getActiveIngredient(),
                entity.getTotalDailyDoseMg(),
                entity.getMaxDailyDoseMg(),
                entity.getOverrideReason(),
                entity.getHandledBy(),
                entity.getHandledAt(),
                entity.getCreatedAt()
        );
    }

    public PrescriptionMaxDailyDoseWarningLogEntity toEntity(PrescriptionMaxDailyDoseWarningLog domain) {
        if (domain == null) {
            return null;
        }
        return PrescriptionMaxDailyDoseWarningLogEntity.builder()
                .id(domain.getId())
                .prescriptionId(domain.getPrescriptionId())
                .patientId(domain.getPatientId())
                .activeIngredient(domain.getActiveIngredient())
                .totalDailyDoseMg(domain.getTotalDailyDoseMg())
                .maxDailyDoseMg(domain.getMaxDailyDoseMg())
                .overrideReason(domain.getOverrideReason())
                .handledBy(domain.getHandledBy())
                .handledAt(domain.getHandledAt())
                .createdAt(domain.getCreatedAt())
                .build();
    }
}

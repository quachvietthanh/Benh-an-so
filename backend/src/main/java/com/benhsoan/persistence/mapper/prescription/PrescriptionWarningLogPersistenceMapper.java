package com.benhsoan.persistence.mapper.prescription;

import org.springframework.stereotype.Component;

import com.benhsoan.domain.prescription.PrescriptionWarningLog;
import com.benhsoan.persistence.entity.prescription.PrescriptionWarningLogEntity;

@Component
public class PrescriptionWarningLogPersistenceMapper {

    public PrescriptionWarningLog toDomain(PrescriptionWarningLogEntity entity) {
        if (entity == null) {
            return null;
        }

        return PrescriptionWarningLog.restore(
                entity.getId(),
                entity.getPrescriptionId(),
                entity.getRuleId(),
                entity.getFirstMedicineId(),
                entity.getSecondMedicineId(),
                entity.getSeverity(),
                entity.getWarningMessage(),
                entity.getAction(),
                entity.getOverrideReason(),
                entity.getHandledBy(),
                entity.getHandledAt(),
                entity.getCreatedAt()
        );
    }

    public PrescriptionWarningLogEntity toEntity(PrescriptionWarningLog domain) {
        if (domain == null) {
            return null;
        }

        return PrescriptionWarningLogEntity.builder()
                .id(domain.getId())
                .prescriptionId(domain.getPrescriptionId())
                .ruleId(domain.getRuleId())
                .firstMedicineId(domain.getFirstMedicineId())
                .secondMedicineId(domain.getSecondMedicineId())
                .severity(domain.getSeverity())
                .warningMessage(domain.getWarningMessage())
                .action(domain.getAction())
                .overrideReason(domain.getOverrideReason())
                .handledBy(domain.getHandledBy())
                .handledAt(domain.getHandledAt())
                .createdAt(domain.getCreatedAt())
                .build();
    }
}

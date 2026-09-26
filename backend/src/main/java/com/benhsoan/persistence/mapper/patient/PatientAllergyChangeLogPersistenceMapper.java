package com.benhsoan.persistence.mapper.patient;

import org.springframework.stereotype.Component;

import com.benhsoan.domain.patient.PatientAllergyChangeLog;
import com.benhsoan.persistence.entity.patient.PatientAllergyChangeLogEntity;

@Component
public class PatientAllergyChangeLogPersistenceMapper {

    public PatientAllergyChangeLog toDomain(PatientAllergyChangeLogEntity entity) {
        if (entity == null) {
            return null;
        }

        return PatientAllergyChangeLog.restore(
                entity.getId(),
                entity.getAllergyId(),
                entity.getPatientId(),
                entity.getAction(),
                entity.getBeforeData(),
                entity.getAfterData(),
                entity.getChangeReason(),
                entity.getChangedBy(),
                entity.getChangedAt()
        );
    }

    public PatientAllergyChangeLogEntity toEntity(PatientAllergyChangeLog domain) {
        if (domain == null) {
            return null;
        }

        return PatientAllergyChangeLogEntity.builder()
                .id(domain.getId())
                .allergyId(domain.getAllergyId())
                .patientId(domain.getPatientId())
                .action(domain.getAction())
                .beforeData(domain.getBeforeData())
                .afterData(domain.getAfterData())
                .changeReason(domain.getChangeReason())
                .changedBy(domain.getChangedBy())
                .changedAt(domain.getChangedAt())
                .build();
    }
}

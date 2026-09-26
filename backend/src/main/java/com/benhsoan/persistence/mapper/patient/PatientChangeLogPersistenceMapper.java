package com.benhsoan.persistence.mapper.patient;

import org.springframework.stereotype.Component;

import com.benhsoan.domain.patient.PatientChangeLog;
import com.benhsoan.persistence.entity.patient.PatientChangeLogEntity;

@Component
public class PatientChangeLogPersistenceMapper {

    public PatientChangeLog toDomain(PatientChangeLogEntity entity) {

        if (entity == null) {
            return null;
        }

        return PatientChangeLog.restore(
                entity.getId(),
                entity.getPatientId(),
                entity.getChangedBy(),
                entity.getAction(),
                entity.getChangeDetail(),
                entity.getCreatedAt()
        );
    }

    public PatientChangeLogEntity toEntity(PatientChangeLog domain) {

        if (domain == null) {
            return null;
        }

        return PatientChangeLogEntity.builder()
                .id(domain.getId())
                .patientId(domain.getPatientId())
                .changedBy(domain.getChangedBy())
                .action(domain.getAction())
                .changeDetail(domain.getChangeDetail())
                .createdAt(domain.getCreatedAt())
                .build();
    }

}
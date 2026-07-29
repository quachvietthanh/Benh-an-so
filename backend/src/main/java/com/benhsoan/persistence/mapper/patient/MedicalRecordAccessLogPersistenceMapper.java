package com.benhsoan.persistence.mapper.patient;

import org.springframework.stereotype.Component;

import com.benhsoan.domain.patient.MedicalRecordAccessLog;
import com.benhsoan.persistence.entity.patient.MedicalRecordAccessLogEntity;

@Component
public class MedicalRecordAccessLogPersistenceMapper {

    public MedicalRecordAccessLog toDomain(
            MedicalRecordAccessLogEntity entity
    ) {

        if (entity == null) {
            return null;
        }

        return MedicalRecordAccessLog.restore(
                entity.getId(),
                entity.getPatientId(),
                entity.getVisitId(),
                entity.getAccessedBy(),
                entity.getAction(),
                null,
                entity.getAccessedAt()
        );
    }

    public MedicalRecordAccessLogEntity toEntity(
            MedicalRecordAccessLog domain
    ) {

        if (domain == null) {
            return null;
        }

        return MedicalRecordAccessLogEntity.builder()
                .id(domain.getId())
                .patientId(domain.getPatientId())
                .visitId(domain.getVisitId())
                .accessedBy(domain.getAccessedBy())
                .action(domain.getAction())
                .ipAddress(null)
                .accessedAt(domain.getAccessedAt())
                .build();
    }

}

package com.benhsoan.persistence.mapper.patient;

import org.springframework.stereotype.Component;

import com.benhsoan.domain.patient.PatientFamilyHistory;
import com.benhsoan.persistence.entity.patient.PatientFamilyHistoryEntity;

@Component
public class PatientFamilyHistoryPersistenceMapper {

    public PatientFamilyHistory toDomain(PatientFamilyHistoryEntity entity) {
        if (entity == null) {
            return null;
        }
        return PatientFamilyHistory.restore(
                entity.getId(),
                entity.getPatientId(),
                entity.getRelationship(),
                entity.getDiagnosisCatalogId(),
                entity.getNotes(),
                entity.isActive(),
                entity.getCreatedBy(),
                entity.getCreatedAt(),
                entity.getUpdatedBy(),
                entity.getUpdatedAt()
        );
    }

    public PatientFamilyHistoryEntity toEntity(PatientFamilyHistory domain) {
        if (domain == null) {
            return null;
        }
        return PatientFamilyHistoryEntity.builder()
                .id(domain.getId())
                .patientId(domain.getPatientId())
                .relationship(domain.getRelationship())
                .diagnosisCatalogId(domain.getDiagnosisCatalogId())
                .notes(domain.getNotes())
                .active(domain.isActive())
                .createdBy(domain.getCreatedBy())
                .createdAt(domain.getCreatedAt())
                .updatedBy(domain.getUpdatedBy())
                .updatedAt(domain.getUpdatedAt())
                .build();
    }
}

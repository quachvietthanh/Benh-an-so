package com.benhsoan.persistence.mapper.patient;

import org.springframework.stereotype.Component;

import com.benhsoan.domain.patient.PatientChronicDisease;
import com.benhsoan.persistence.entity.patient.PatientChronicDiseaseEntity;

@Component
public class PatientChronicDiseasePersistenceMapper {

    public PatientChronicDisease toDomain(PatientChronicDiseaseEntity entity) {
        if (entity == null) {
            return null;
        }
        return PatientChronicDisease.restore(
                entity.getId(),
                entity.getPatientId(),
                entity.getDiagnosisCatalogId(),
                entity.getYearDetected(),
                entity.getNotes(),
                entity.isActive(),
                entity.getCreatedBy(),
                entity.getCreatedAt(),
                entity.getUpdatedBy(),
                entity.getUpdatedAt()
        );
    }

    public PatientChronicDiseaseEntity toEntity(PatientChronicDisease domain) {
        if (domain == null) {
            return null;
        }
        return PatientChronicDiseaseEntity.builder()
                .id(domain.getId())
                .patientId(domain.getPatientId())
                .diagnosisCatalogId(domain.getDiagnosisCatalogId())
                .yearDetected(domain.getYearDetected())
                .notes(domain.getNotes())
                .active(domain.isActive())
                .createdBy(domain.getCreatedBy())
                .createdAt(domain.getCreatedAt())
                .updatedBy(domain.getUpdatedBy())
                .updatedAt(domain.getUpdatedAt())
                .build();
    }
}

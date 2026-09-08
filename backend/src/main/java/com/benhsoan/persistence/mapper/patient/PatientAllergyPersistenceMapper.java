package com.benhsoan.persistence.mapper.patient;

import org.springframework.stereotype.Component;

import com.benhsoan.domain.patient.PatientAllergy;
import com.benhsoan.persistence.entity.patient.PatientAllergyEntity;

@Component
public class PatientAllergyPersistenceMapper {

    public PatientAllergy toDomain(PatientAllergyEntity entity) {
        if (entity == null) {
            return null;
        }

        return PatientAllergy.restore(
                entity.getId(),
                entity.getPatientId(),
                entity.getAllergenType(),
                entity.getAllergenName(),
                entity.getNormalizedAllergenName(),
                entity.getSeverity(),
                entity.getReaction(),
                entity.getNotes(),
                entity.isActive(),
                entity.getCreatedBy(),
                entity.getCreatedAt(),
                entity.getUpdatedBy(),
                entity.getUpdatedAt()
        );
    }

    public PatientAllergyEntity toEntity(PatientAllergy domain) {
        if (domain == null) {
            return null;
        }

        return PatientAllergyEntity.builder()
                .id(domain.getId())
                .patientId(domain.getPatientId())
                .allergenType(domain.getAllergenType())
                .allergenName(domain.getAllergenName())
                .normalizedAllergenName(domain.getNormalizedAllergenName())
                .severity(domain.getSeverity())
                .reaction(domain.getReaction())
                .notes(domain.getNotes())
                .active(domain.isActive())
                .createdBy(domain.getCreatedBy())
                .createdAt(domain.getCreatedAt())
                .updatedBy(domain.getUpdatedBy())
                .updatedAt(domain.getUpdatedAt())
                .build();
    }
}

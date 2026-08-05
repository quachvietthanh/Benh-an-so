package com.benhsoan.persistence.mapper.druginteraction;

import org.springframework.stereotype.Component;

import com.benhsoan.domain.druginteraction.DrugInteraction;
import com.benhsoan.persistence.entity.druginteraction.DrugInteractionEntity;

@Component
public class DrugInteractionPersistenceMapper {

    public DrugInteraction toDomain(DrugInteractionEntity entity) {
        if (entity == null) {
            return null;
        }

        return DrugInteraction.restore(
                entity.getId(),
                entity.getFirstMedicineId(),
                entity.getSecondMedicineId(),
                entity.getSeverity(),
                entity.getDescription(),
                entity.getRecommendation(),
                entity.isActive(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    public DrugInteractionEntity toEntity(DrugInteraction domain) {
        if (domain == null) {
            return null;
        }

        return DrugInteractionEntity.builder()
                .id(domain.getId())
                .firstMedicineId(domain.getFirstMedicineId())
                .secondMedicineId(domain.getSecondMedicineId())
                .severity(domain.getSeverity())
                .description(domain.getDescription())
                .recommendation(domain.getRecommendation())
                .active(domain.isActive())
                .createdAt(domain.getCreatedAt())
                .updatedAt(domain.getUpdatedAt())
                .build();
    }
}

package com.benhsoan.persistence.mapper.contraindication;

import org.springframework.stereotype.Component;

import com.benhsoan.domain.contraindication.ContraindicationRule;
import com.benhsoan.persistence.entity.contraindication.ContraindicationRuleEntity;

@Component
public class ContraindicationRulePersistenceMapper {

    public ContraindicationRule toDomain(ContraindicationRuleEntity entity) {
        if (entity == null) {
            return null;
        }
        return ContraindicationRule.restore(
                entity.getId(),
                entity.getMedicineId(),
                entity.getActiveIngredient(),
                entity.getType(),
                entity.getMinAgeYears(),
                entity.getMaxAgeYears(),
                entity.getDiagnosisCatalogId(),
                entity.getSeverity(),
                entity.getMessage(),
                entity.getRecommendation(),
                entity.isActive(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    public ContraindicationRuleEntity toEntity(ContraindicationRule domain) {
        if (domain == null) {
            return null;
        }
        return ContraindicationRuleEntity.builder()
                .id(domain.getId())
                .medicineId(domain.getMedicineId())
                .activeIngredient(domain.getActiveIngredient())
                .type(domain.getType())
                .minAgeYears(domain.getMinAgeYears())
                .maxAgeYears(domain.getMaxAgeYears())
                .diagnosisCatalogId(domain.getDiagnosisCatalogId())
                .severity(domain.getSeverity())
                .message(domain.getMessage())
                .recommendation(domain.getRecommendation())
                .active(domain.isActive())
                .createdAt(domain.getCreatedAt())
                .updatedAt(domain.getUpdatedAt())
                .build();
    }
}

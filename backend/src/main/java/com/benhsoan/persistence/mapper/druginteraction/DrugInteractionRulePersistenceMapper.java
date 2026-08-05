package com.benhsoan.persistence.mapper.druginteraction;

import org.springframework.stereotype.Component;

import com.benhsoan.domain.druginteraction.DrugInteractionRule;
import com.benhsoan.persistence.entity.druginteraction.DrugInteractionRuleEntity;

@Component
public class DrugInteractionRulePersistenceMapper {

    public DrugInteractionRule toDomain(DrugInteractionRuleEntity entity) {
        if (entity == null) {
            return null;
        }

        return DrugInteractionRule.restore(
                entity.getId(),
                entity.getActiveIngredientA(),
                entity.getActiveIngredientB(),
                entity.getSeverity(),
                entity.getDescription(),
                entity.getClinicalRecommendation(),
                entity.isActive(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    public DrugInteractionRuleEntity toEntity(DrugInteractionRule domain) {
        if (domain == null) {
            return null;
        }

        return DrugInteractionRuleEntity.builder()
                .id(domain.getId())
                .activeIngredientA(domain.getActiveIngredientA())
                .activeIngredientB(domain.getActiveIngredientB())
                .severity(domain.getSeverity())
                .description(domain.getDescription())
                .clinicalRecommendation(domain.getClinicalRecommendation())
                .active(domain.isActive())
                .createdAt(domain.getCreatedAt())
                .updatedAt(domain.getUpdatedAt())
                .build();
    }
}

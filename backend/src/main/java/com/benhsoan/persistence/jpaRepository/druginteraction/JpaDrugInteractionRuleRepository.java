package com.benhsoan.persistence.jpaRepository.druginteraction;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.benhsoan.persistence.entity.druginteraction.DrugInteractionRuleEntity;

public interface JpaDrugInteractionRuleRepository
        extends JpaRepository<DrugInteractionRuleEntity, UUID> {

    @Query("""
            select rule from DrugInteractionRuleEntity rule
            where rule.active = true
              and ((rule.activeIngredientA = :ingredientA
                      and rule.activeIngredientB = :ingredientB)
                 or (rule.activeIngredientA = :ingredientB
                      and rule.activeIngredientB = :ingredientA))
            """)
    Optional<DrugInteractionRuleEntity> findActiveRuleBetween(
            @Param("ingredientA") String ingredientA,
            @Param("ingredientB") String ingredientB
    );
}

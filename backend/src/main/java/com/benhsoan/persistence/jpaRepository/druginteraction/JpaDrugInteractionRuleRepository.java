package com.benhsoan.persistence.jpaRepository.druginteraction;

import java.util.List;
import java.util.Optional;
import java.util.Set;
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

    @Query("""
            select rule from DrugInteractionRuleEntity rule
            where rule.active = true
              and (rule.activeIngredientA in :ingredients
                   or rule.activeIngredientB in :ingredients)
            """)
    List<DrugInteractionRuleEntity> findActiveRulesByIngredients(
            @Param("ingredients") Set<String> ingredients
    );
}

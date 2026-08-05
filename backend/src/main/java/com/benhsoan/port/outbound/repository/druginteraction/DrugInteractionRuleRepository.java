package com.benhsoan.port.outbound.repository.druginteraction;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.benhsoan.domain.druginteraction.DrugInteractionRule;

public interface DrugInteractionRuleRepository {

    Optional<DrugInteractionRule> findActiveRuleBetween(
            String activeIngredientA,
            String activeIngredientB
    );

    List<DrugInteractionRule> findActiveRulesByIngredients(
            Set<String> activeIngredients
    );
}

package com.benhsoan.port.outbound.repository.druginteraction;

import java.util.Optional;

import com.benhsoan.domain.druginteraction.DrugInteractionRule;

public interface DrugInteractionRuleRepository {

    Optional<DrugInteractionRule> findActiveRuleBetween(
            String activeIngredientA,
            String activeIngredientB
    );
}

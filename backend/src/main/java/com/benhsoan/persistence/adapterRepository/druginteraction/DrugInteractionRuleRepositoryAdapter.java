package com.benhsoan.persistence.adapterRepository.druginteraction;

import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.benhsoan.domain.druginteraction.DrugInteractionRule;
import com.benhsoan.persistence.jpaRepository.druginteraction.JpaDrugInteractionRuleRepository;
import com.benhsoan.persistence.mapper.druginteraction.DrugInteractionRulePersistenceMapper;
import com.benhsoan.port.outbound.repository.druginteraction.DrugInteractionRuleRepository;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class DrugInteractionRuleRepositoryAdapter
        implements DrugInteractionRuleRepository {

    private final JpaDrugInteractionRuleRepository jpaRepository;

    private final DrugInteractionRulePersistenceMapper mapper;

    @Override
    public Optional<DrugInteractionRule> findActiveRuleBetween(
            String activeIngredientA,
            String activeIngredientB
    ) {
        if (activeIngredientA == null || activeIngredientA.isBlank()
                || activeIngredientB == null || activeIngredientB.isBlank()) {
            return Optional.empty();
        }

        return jpaRepository.findActiveRuleBetween(
                activeIngredientA.trim(),
                activeIngredientB.trim()
        ).map(mapper::toDomain);
    }
}

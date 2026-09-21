package com.benhsoan.persistence.adapterRepository.contraindication;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.benhsoan.domain.contraindication.ContraindicationRule;
import com.benhsoan.persistence.jpaRepository.contraindication.JpaContraindicationRuleRepository;
import com.benhsoan.persistence.mapper.contraindication.ContraindicationRulePersistenceMapper;
import com.benhsoan.port.outbound.repository.contraindication.ContraindicationRuleRepository;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class ContraindicationRuleRepositoryAdapter implements ContraindicationRuleRepository {

    private final JpaContraindicationRuleRepository jpaRepository;
    private final ContraindicationRulePersistenceMapper mapper;

    @Override
    public List<ContraindicationRule> findActiveByMedicineIdsAndIngredients(
            Collection<UUID> medicineIds,
            Collection<String> activeIngredients
    ) {
        return jpaRepository.findActiveByMedicineIdsAndIngredients(medicineIds, activeIngredients)
                .stream()
                .map(mapper::toDomain)
                .toList();
    }
}

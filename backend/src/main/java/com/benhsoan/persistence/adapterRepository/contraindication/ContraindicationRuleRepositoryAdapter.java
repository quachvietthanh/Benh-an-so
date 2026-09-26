package com.benhsoan.persistence.adapterRepository.contraindication;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import com.benhsoan.domain.contraindication.ContraindicationRule;
import com.benhsoan.domain.contraindication.enums.ContraindicationSeverity;
import com.benhsoan.domain.contraindication.enums.ContraindicationType;
import com.benhsoan.persistence.entity.contraindication.ContraindicationRuleEntity;
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

    @Override
    public Optional<ContraindicationRule> findById(UUID id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public ContraindicationRule save(ContraindicationRule rule) {
        ContraindicationRuleEntity entity = mapper.toEntity(rule);
        return mapper.toDomain(jpaRepository.save(entity));
    }

    @Override
    public Page<ContraindicationRule> search(
            String activeIngredient,
            ContraindicationType type,
            ContraindicationSeverity severity,
            Boolean active,
            Pageable pageable
    ) {
        return jpaRepository.search(activeIngredient, type, severity, active, pageable)
                .map(mapper::toDomain);
    }
}

package com.benhsoan.port.outbound.repository.contraindication;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.benhsoan.domain.contraindication.ContraindicationRule;
import com.benhsoan.domain.contraindication.enums.ContraindicationSeverity;
import com.benhsoan.domain.contraindication.enums.ContraindicationType;

public interface ContraindicationRuleRepository {

    List<ContraindicationRule> findActiveByMedicineIdsAndIngredients(
            Collection<UUID> medicineIds,
            Collection<String> activeIngredients
    );

    Optional<ContraindicationRule> findById(UUID id);

    ContraindicationRule save(ContraindicationRule rule);

    Page<ContraindicationRule> search(
            String activeIngredient,
            ContraindicationType type,
            ContraindicationSeverity severity,
            Boolean active,
            Pageable pageable
    );
}

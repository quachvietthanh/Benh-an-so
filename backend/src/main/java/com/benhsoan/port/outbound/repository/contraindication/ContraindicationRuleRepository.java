package com.benhsoan.port.outbound.repository.contraindication;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import com.benhsoan.domain.contraindication.ContraindicationRule;

public interface ContraindicationRuleRepository {

    List<ContraindicationRule> findActiveByMedicineIdsAndIngredients(
            Collection<UUID> medicineIds,
            Collection<String> activeIngredients
    );
}

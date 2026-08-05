package com.benhsoan.port.outbound.repository.druginteraction;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.benhsoan.domain.druginteraction.DrugInteraction;
public interface DrugInteractionRepository {

    Optional<DrugInteraction> findByMedicinePair(
            UUID firstMedicineId,
            UUID secondMedicineId
    );

    Optional<DrugInteraction> findActiveByMedicinePair(
            UUID firstMedicineId,
            UUID secondMedicineId
    );

    boolean existsByMedicinePair(
            UUID firstMedicineId,
            UUID secondMedicineId
    );

    List<DrugInteraction> findActiveInteractionsAmong(
            Collection<UUID> medicineIds
    );
}

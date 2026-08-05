package com.benhsoan.persistence.adapterRepository.druginteraction;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.benhsoan.domain.druginteraction.DrugInteraction;
import com.benhsoan.persistence.jpaRepository.druginteraction.JpaDrugInteractionRepository;
import com.benhsoan.persistence.mapper.druginteraction.DrugInteractionPersistenceMapper;
import com.benhsoan.port.outbound.repository.druginteraction.DrugInteractionRepository;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class DrugInteractionRepositoryAdapter
        implements DrugInteractionRepository {

    private final JpaDrugInteractionRepository jpaRepository;

    private final DrugInteractionPersistenceMapper mapper;

    @Override
    public Optional<DrugInteraction> findByMedicinePair(
            UUID firstMedicineId,
            UUID secondMedicineId
    ) {
        MedicinePair pair = normalizePair(firstMedicineId, secondMedicineId);

        return jpaRepository.findByFirstMedicineIdAndSecondMedicineId(
                pair.firstMedicineId(),
                pair.secondMedicineId()
        ).map(mapper::toDomain);
    }

    @Override
    public Optional<DrugInteraction> findActiveByMedicinePair(
            UUID firstMedicineId,
            UUID secondMedicineId
    ) {
        MedicinePair pair = normalizePair(firstMedicineId, secondMedicineId);

        return jpaRepository.findByFirstMedicineIdAndSecondMedicineIdAndActiveTrue(
                pair.firstMedicineId(),
                pair.secondMedicineId()
        ).map(mapper::toDomain);
    }

    @Override
    public boolean existsByMedicinePair(
            UUID firstMedicineId,
            UUID secondMedicineId
    ) {
        MedicinePair pair = normalizePair(firstMedicineId, secondMedicineId);

        return jpaRepository.existsByFirstMedicineIdAndSecondMedicineId(
                pair.firstMedicineId(),
                pair.secondMedicineId()
        );
    }

    @Override
    public List<DrugInteraction> findActiveInteractionsAmong(
            Collection<UUID> medicineIds
    ) {
        if (medicineIds == null || medicineIds.size() < 2) {
            return List.of();
        }

        return jpaRepository.findActiveInteractionsAmong(medicineIds)
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    private static MedicinePair normalizePair(
            UUID firstMedicineId,
            UUID secondMedicineId
    ) {
        if (firstMedicineId == null || secondMedicineId == null) {
            throw new IllegalArgumentException("Both medicine ids are required.");
        }

        if (firstMedicineId.compareTo(secondMedicineId) <= 0) {
            return new MedicinePair(firstMedicineId, secondMedicineId);
        }

        return new MedicinePair(secondMedicineId, firstMedicineId);
    }

    private record MedicinePair(
            UUID firstMedicineId,
            UUID secondMedicineId
    ) {
    }
}

package com.benhsoan.application.ucservice.inventory;

import java.time.LocalDate;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.benhsoan.domain.inventory.MedicineBatch;
import com.benhsoan.port.outbound.repository.inventory.MedicineBatchRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EligibleStockSnapshotService {

    private final MedicineBatchRepository medicineBatchRepository;
    private final LowStockEvaluator lowStockEvaluator;

    public Map<UUID, Integer> snapshotEligibleStockQuantities(Collection<UUID> medicineIds, LocalDate today) {
        Map<UUID, Integer> eligibleStockByMedicineId = new LinkedHashMap<>();
        if (medicineIds == null || medicineIds.isEmpty()) {
            return eligibleStockByMedicineId;
        }

        List<UUID> validMedicineIds = medicineIds.stream()
                .filter(Objects::nonNull)
                .toList();
        if (validMedicineIds.isEmpty()) {
            return eligibleStockByMedicineId;
        }

        Map<UUID, List<MedicineBatch>> batchesByMedicineId = medicineBatchRepository
                .findByMedicineIdIn(validMedicineIds)
                .stream()
                .collect(Collectors.groupingBy(MedicineBatch::getMedicineId));

        for (UUID medicineId : validMedicineIds) {
            List<MedicineBatch> batches = batchesByMedicineId.getOrDefault(medicineId, List.of());
            eligibleStockByMedicineId.put(
                    medicineId,
                    lowStockEvaluator.calculateEligibleStockQuantity(batches, today)
            );
        }
        return eligibleStockByMedicineId;
    }
}

package com.benhsoan.application.ucservice.inventory;

import java.time.LocalDate;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;

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

        for (UUID medicineId : medicineIds) {
            if (medicineId == null) {
                continue;
            }
            eligibleStockByMedicineId.put(
                    medicineId,
                    lowStockEvaluator.calculateEligibleStockQuantity(
                            medicineBatchRepository.findByMedicineId(medicineId),
                            today
                    )
            );
        }
        return eligibleStockByMedicineId;
    }
}

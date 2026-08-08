package com.benhsoan.application.ucservice.inventory;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.domain.inventory.InventoryAlertLog;
import com.benhsoan.domain.inventory.enums.InventoryAlertType;
import com.benhsoan.domain.medicine.Medicine;
import com.benhsoan.port.outbound.repository.inventory.InventoryAlertLogRepository;
import com.benhsoan.port.outbound.repository.medicine.MedicineRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class LowStockAlertTransitionService {

    private final MedicineRepository medicineRepository;
    private final InventoryAlertLogRepository inventoryAlertLogRepository;
    private final LowStockEvaluator lowStockEvaluator;

    public void handleEligibleStockTransitions(
            Map<UUID, Integer> beforeEligibleQuantities,
            Map<UUID, Integer> afterEligibleQuantities,
            Instant now
    ) {
        if (afterEligibleQuantities == null || afterEligibleQuantities.isEmpty()) {
            return;
        }

        Map<UUID, Medicine> medicinesById = medicineRepository.findAllById(afterEligibleQuantities.keySet())
                .stream()
                .collect(Collectors.toMap(Medicine::getId, medicine -> medicine));

        for (Map.Entry<UUID, Integer> entry : afterEligibleQuantities.entrySet()) {
            UUID medicineId = entry.getKey();
            Medicine medicine = medicinesById.get(medicineId);
            if (medicine == null || !medicine.isActive()) {
                continue;
            }

            int beforeQuantity = beforeEligibleQuantities.getOrDefault(medicineId, 0);
            int afterQuantity = entry.getValue();

            boolean wasLow = lowStockEvaluator.isLowStock(medicine, beforeQuantity);
            boolean isLow = lowStockEvaluator.isLowStock(medicine, afterQuantity);

            if (!wasLow && isLow) {
                boolean hasActiveAlert = inventoryAlertLogRepository.findActiveByMedicineIdAndAlertType(
                        medicineId,
                        InventoryAlertType.LOW_STOCK
                ).isPresent();
                if (!hasActiveAlert) {
                    inventoryAlertLogRepository.save(
                            InventoryAlertLog.createLowStock(
                                    medicineId,
                                    medicine.getMinStockThreshold(),
                                    afterQuantity,
                                    now
                            )
                    );
                }
                continue;
            }

            if (wasLow && !isLow) {
                inventoryAlertLogRepository.findActiveByMedicineIdAndAlertType(
                                medicineId,
                                InventoryAlertType.LOW_STOCK
                        )
                        .ifPresent(alertLog -> {
                            alertLog.resolve(now);
                            inventoryAlertLogRepository.save(alertLog);
                        });
            }
        }
    }
}

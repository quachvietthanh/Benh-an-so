package com.benhsoan.port.outbound.repository.inventory;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.benhsoan.domain.inventory.StockMovement;
import com.benhsoan.domain.inventory.enums.StockMovementReferenceType;
import com.benhsoan.port.dto.result.inventory.MedicineMovementSummaryResult;
import com.benhsoan.port.dto.result.inventory.MedicineStockQuantityResult;

public interface StockMovementRepository {

    StockMovement save(StockMovement stockMovement);

    List<StockMovement> saveAll(List<StockMovement> stockMovements);

    Optional<StockMovement> findById(UUID id);

    List<StockMovement> findByMedicineBatchId(UUID medicineBatchId);

    List<StockMovement> findByReference(StockMovementReferenceType referenceType, UUID referenceId);

    List<MedicineStockQuantityResult> sumQuantitiesBefore(Instant beforeInstant);

    List<MedicineStockQuantityResult> sumQuantitiesBeforeForMedicine(UUID medicineId, Instant beforeInstant);

    List<MedicineStockQuantityResult> sumQuantitiesBeforeForMedicineIds(Collection<UUID> medicineIds, Instant beforeInstant);

    List<MedicineMovementSummaryResult> sumMovementsBetween(Instant from, Instant to);

    List<MedicineMovementSummaryResult> sumMovementsBetweenForMedicine(UUID medicineId, Instant from, Instant to);

    List<MedicineMovementSummaryResult> sumMovementsBetweenForMedicineIds(Collection<UUID> medicineIds, Instant from, Instant to);
}


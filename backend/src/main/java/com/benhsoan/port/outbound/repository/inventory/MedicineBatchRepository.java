package com.benhsoan.port.outbound.repository.inventory;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.benhsoan.domain.inventory.enums.BatchStatus;
import com.benhsoan.domain.inventory.MedicineBatch;

public interface MedicineBatchRepository {

    Optional<MedicineBatch> findByMedicineIdAndBatchNumber(UUID medicineId, String batchNumber);

    List<MedicineBatch> findAvailableByMedicineId(UUID medicineId, LocalDate today);

    List<MedicineBatch> findAvailableByMedicineIdForUpdate(UUID medicineId, LocalDate today);

    MedicineBatch save(MedicineBatch batch);

    void addStockQuantity(UUID batchId, int delta);

    void deductStockQuantity(UUID batchId, int delta, BatchStatus status, Instant updatedAt);
}

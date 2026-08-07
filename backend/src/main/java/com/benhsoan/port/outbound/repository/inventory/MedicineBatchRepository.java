package com.benhsoan.port.outbound.repository.inventory;

import java.util.Optional;
import java.util.UUID;

import com.benhsoan.domain.inventory.MedicineBatch;

public interface MedicineBatchRepository {

    Optional<MedicineBatch> findByMedicineIdAndBatchNumber(UUID medicineId, String batchNumber);

    MedicineBatch save(MedicineBatch batch);

    void addStockQuantity(UUID batchId, int delta);
}

package com.benhsoan.port.outbound.repository.inventory;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.benhsoan.domain.inventory.StockMovement;
import com.benhsoan.domain.inventory.enums.StockMovementReferenceType;

public interface StockMovementRepository {

    StockMovement save(StockMovement stockMovement);

    List<StockMovement> saveAll(List<StockMovement> stockMovements);

    Optional<StockMovement> findById(UUID id);

    List<StockMovement> findByMedicineBatchId(UUID medicineBatchId);

    List<StockMovement> findByReference(StockMovementReferenceType referenceType, UUID referenceId);
}

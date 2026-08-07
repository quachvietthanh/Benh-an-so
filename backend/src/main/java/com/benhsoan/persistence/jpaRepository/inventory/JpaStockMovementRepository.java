package com.benhsoan.persistence.jpaRepository.inventory;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.benhsoan.domain.inventory.enums.StockMovementReferenceType;
import com.benhsoan.persistence.entity.inventory.StockMovementEntity;

public interface JpaStockMovementRepository
        extends JpaRepository<StockMovementEntity, UUID> {

    List<StockMovementEntity> findByMedicineBatchIdOrderByPerformedAtAsc(UUID medicineBatchId);

    List<StockMovementEntity> findByReferenceTypeAndReferenceIdOrderByPerformedAtAsc(
            StockMovementReferenceType referenceType,
            UUID referenceId
    );
}

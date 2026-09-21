package com.benhsoan.persistence.jpaRepository.inventory;

import java.util.UUID;

import com.benhsoan.domain.inventory.enums.StockMovementType;

public interface MedicinePeriodMovementProjection {

    UUID getMedicineId();

    StockMovementType getMovementType();

    Long getTotalQuantityChange();
}

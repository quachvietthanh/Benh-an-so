package com.benhsoan.persistence.jpaRepository.inventory;

import java.util.UUID;

public interface MedicineStockQuantityProjection {

    UUID getMedicineId();

    Long getTotalQuantity();
}

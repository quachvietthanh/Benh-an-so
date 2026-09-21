package com.benhsoan.persistence.adapterRepository.inventory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import com.benhsoan.domain.inventory.enums.BatchStatus;
import com.benhsoan.domain.inventory.enums.StockMovementReferenceType;
import com.benhsoan.domain.inventory.enums.StockMovementType;
import com.benhsoan.persistence.entity.inventory.MedicineBatchEntity;
import com.benhsoan.persistence.entity.inventory.StockMovementEntity;
import com.benhsoan.persistence.jpaRepository.inventory.JpaMedicineBatchRepository;
import com.benhsoan.persistence.jpaRepository.inventory.JpaStockMovementRepository;

@DataJpaTest(properties = {
        "spring.flyway.enabled=false",
        "spring.sql.init.mode=never",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@DisplayName("Inventory Adjustment Persistence & JPA Lock Integration Tests")
class InventoryAdjustmentPersistenceIntegrationTest {

    @Autowired
    private JpaMedicineBatchRepository batchRepository;

    @Autowired
    private JpaStockMovementRepository stockMovementRepository;

    @Test
    @DisplayName("findByIdForUpdate should lock and retrieve batch entity successfully")
    void findByIdForUpdateShouldRetrieveBatchWithLock() {
        UUID batchId = UUID.randomUUID();
        UUID medicineId = UUID.randomUUID();

        MedicineBatchEntity entity = MedicineBatchEntity.builder()
                .id(batchId)
                .medicineId(medicineId)
                .batchNumber("BATCH-LCK-001")
                .expiryDate(LocalDate.of(2027, 12, 31))
                .quantity(100)
                .status(BatchStatus.ACTIVE)
                .createdAt(Instant.now())
                .updatedAt(null)
                .build();

        batchRepository.save(entity);

        Optional<MedicineBatchEntity> lockedBatch = batchRepository.findByIdForUpdate(batchId);

        assertTrue(lockedBatch.isPresent());
        assertEquals("BATCH-LCK-001", lockedBatch.get().getBatchNumber());
        assertEquals(100, lockedBatch.get().getQuantity());
    }

    @Test
    @DisplayName("Persists adjustment stock movement with MANUAL_ADJUSTMENT reference type")
    void persistsAdjustmentStockMovementSuccessfully() {
        UUID movementId = UUID.randomUUID();
        UUID batchId = UUID.randomUUID();
        UUID medicineId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        Instant now = Instant.now();

        StockMovementEntity movement = StockMovementEntity.builder()
                .id(movementId)
                .medicineId(medicineId)
                .medicineBatchId(batchId)
                .movementType(StockMovementType.ADJUSTMENT)
                .referenceType(StockMovementReferenceType.MANUAL_ADJUSTMENT)
                .referenceId(batchId)
                .quantityBefore(100)
                .quantityAfter(85)
                .quantityChange(-15)
                .performedBy(actorId)
                .performedAt(now)
                .createdAt(now)
                .note("Kiểm kê thực tế")
                .build();

        StockMovementEntity saved = stockMovementRepository.save(movement);

        assertNotNull(saved);
        Optional<StockMovementEntity> retrieved = stockMovementRepository.findById(movementId);
        assertTrue(retrieved.isPresent());
        assertEquals(StockMovementType.ADJUSTMENT, retrieved.get().getMovementType());
        assertEquals(StockMovementReferenceType.MANUAL_ADJUSTMENT, retrieved.get().getReferenceType());
        assertEquals(-15, retrieved.get().getQuantityChange());
        assertEquals(batchId, retrieved.get().getReferenceId());
    }

    @Test
    @DisplayName("Persists expire stock movement with EXPIRY_PROCESS reference type")
    void persistsExpireStockMovementSuccessfully() {
        UUID movementId = UUID.randomUUID();
        UUID batchId = UUID.randomUUID();
        UUID medicineId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        Instant now = Instant.now();

        StockMovementEntity movement = StockMovementEntity.builder()
                .id(movementId)
                .medicineId(medicineId)
                .medicineBatchId(batchId)
                .movementType(StockMovementType.EXPIRE)
                .referenceType(StockMovementReferenceType.EXPIRY_PROCESS)
                .referenceId(batchId)
                .quantityBefore(50)
                .quantityAfter(0)
                .quantityChange(-50)
                .performedBy(actorId)
                .performedAt(now)
                .createdAt(now)
                .note("Hủy hết hạn sử dụng")
                .build();

        StockMovementEntity saved = stockMovementRepository.save(movement);

        assertNotNull(saved);
        Optional<StockMovementEntity> retrieved = stockMovementRepository.findById(movementId);
        assertTrue(retrieved.isPresent());
        assertEquals(StockMovementType.EXPIRE, retrieved.get().getMovementType());
        assertEquals(StockMovementReferenceType.EXPIRY_PROCESS, retrieved.get().getReferenceType());
        assertEquals(-50, retrieved.get().getQuantityChange());
        assertEquals(0, retrieved.get().getQuantityAfter());
    }
}

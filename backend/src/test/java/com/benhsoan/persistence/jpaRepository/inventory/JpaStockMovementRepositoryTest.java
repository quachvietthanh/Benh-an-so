package com.benhsoan.persistence.jpaRepository.inventory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import com.benhsoan.domain.inventory.enums.StockMovementReferenceType;
import com.benhsoan.domain.inventory.enums.StockMovementType;
import com.benhsoan.persistence.entity.inventory.StockMovementEntity;

@DataJpaTest(properties = {
        "spring.flyway.enabled=false",
        "spring.sql.init.mode=never",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
class JpaStockMovementRepositoryTest {

    @Autowired
    private JpaStockMovementRepository repository;

    private UUID medA;
    private UUID medB;
    private UUID batchA;
    private UUID batchB;
    private UUID user;

    private final Instant t0 = Instant.parse("2026-08-01T10:00:00Z");
    private final Instant t1 = Instant.parse("2026-08-15T10:00:00Z"); // Start period
    private final Instant t2 = Instant.parse("2026-08-20T10:00:00Z"); // Mid period
    private final Instant t3 = Instant.parse("2026-08-31T23:59:59Z"); // End period

    @BeforeEach
    void setUp() {
        repository.deleteAll();

        medA = UUID.randomUUID();
        medB = UUID.randomUUID();
        batchA = UUID.randomUUID();
        batchB = UUID.randomUUID();
        user = UUID.randomUUID();

        // Movements before period (opening stock)
        // Med A: +100
        saveMovement(medA, batchA, StockMovementType.RECEIPT, 100, 0, 100, t0);
        // Med B: +50
        saveMovement(medB, batchB, StockMovementType.RECEIPT, 50, 0, 50, t0);

        // Movements within period
        // Med A: RECEIPT +200, DISPENSE -40, RETURN +10, ADJUSTMENT -5
        saveMovement(medA, batchA, StockMovementType.RECEIPT, 200, 100, 300, t2);
        saveMovement(medA, batchA, StockMovementType.DISPENSE, -40, 300, 260, t2.plusSeconds(3600));
        saveMovement(medA, batchA, StockMovementType.RETURN, 10, 260, 270, t2.plusSeconds(7200));
        saveMovement(medA, batchA, StockMovementType.ADJUSTMENT, -5, 270, 265, t2.plusSeconds(10800));

        // Med B: DISPENSE -20
        saveMovement(medB, batchB, StockMovementType.DISPENSE, -20, 50, 30, t2);
    }

    @Test
    @DisplayName("sumQuantitiesBeforeForMedicineIds correctly sums opening stock for target medicines")
    void testSumQuantitiesBeforeForMedicineIds() {
        var results = repository.sumQuantitiesBeforeForMedicineIds(List.of(medA), t1);
        assertEquals(1, results.size());
        assertEquals(medA, results.getFirst().getMedicineId());
        assertEquals(100L, results.getFirst().getTotalQuantity());

        var multiResults = repository.sumQuantitiesBeforeForMedicineIds(List.of(medA, medB), t1);
        assertEquals(2, multiResults.size());
    }

    @Test
    @DisplayName("sumMovementsBetweenForMedicineIds correctly aggregates movements by type within period")
    void testSumMovementsBetweenForMedicineIds() {
        var results = repository.sumMovementsBetweenForMedicineIds(List.of(medA), t1, t3);
        assertFalse(results.isEmpty());

        for (var proj : results) {
            assertEquals(medA, proj.getMedicineId());
            assertNotNull(proj.getMovementType());
            if (proj.getMovementType() == StockMovementType.RECEIPT) {
                assertEquals(200L, proj.getTotalQuantityChange());
            } else if (proj.getMovementType() == StockMovementType.DISPENSE) {
                assertEquals(-40L, proj.getTotalQuantityChange());
            } else if (proj.getMovementType() == StockMovementType.RETURN) {
                assertEquals(10L, proj.getTotalQuantityChange());
            } else if (proj.getMovementType() == StockMovementType.ADJUSTMENT) {
                assertEquals(-5L, proj.getTotalQuantityChange());
            }
        }
    }

    @Test
    @DisplayName("sumQuantitiesBefore correctly aggregates all medicines before instant")
    void testSumQuantitiesBefore() {
        var results = repository.sumQuantitiesBefore(t1);
        assertEquals(2, results.size());
    }

    private void saveMovement(
            UUID medicineId,
            UUID batchId,
            StockMovementType type,
            int qtyChange,
            int qtyBefore,
            int qtyAfter,
            Instant performedAt
    ) {
        repository.save(StockMovementEntity.builder()
                .id(UUID.randomUUID())
                .medicineId(medicineId)
                .medicineBatchId(batchId)
                .movementType(type)
                .referenceType(StockMovementReferenceType.INVENTORY_RECEIPT)
                .referenceId(UUID.randomUUID())
                .quantityChange(qtyChange)
                .quantityBefore(qtyBefore)
                .quantityAfter(qtyAfter)
                .performedBy(user)
                .performedAt(performedAt)
                .note("Test movement")
                .createdAt(performedAt)
                .build());
    }
}

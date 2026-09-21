package com.benhsoan.persistence.adapterRepository.inventory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import com.benhsoan.domain.inventory.enums.StockMovementReferenceType;
import com.benhsoan.domain.inventory.enums.StockMovementType;
import com.benhsoan.persistence.entity.inventory.InventoryReceiptEntity;
import com.benhsoan.persistence.entity.inventory.InventoryReceiptItemEntity;
import com.benhsoan.persistence.entity.inventory.StockMovementEntity;
import com.benhsoan.port.outbound.repository.inventory.InventoryStockMovementSummary;

import jakarta.persistence.EntityManager;

@DataJpaTest(properties = {
        "spring.flyway.enabled=false",
        "spring.sql.init.mode=never",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
class InventoryStockReportRepositoryAdapterIntegrationTest {

    private static final Instant FROM = Instant.parse("2026-07-31T17:00:00Z");
    private static final Instant TO = Instant.parse("2026-09-01T17:00:00Z");

    @Autowired
    private EntityManager entityManager;

    @Test
    void aggregatesReceiptDispenseReturnAdjustmentAndOpening() {
        UUID medicineId = UUID.randomUUID();
        UUID batchId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();

        receipt(medicineId, batchId, actorId, FROM.minusSeconds(60), 50);
        receipt(medicineId, batchId, actorId, FROM.plusSeconds(60), 20);
        movement(medicineId, batchId, actorId, FROM.minusSeconds(120), StockMovementType.DISPENSE, -5);
        movement(medicineId, batchId, actorId, FROM.plusSeconds(120), StockMovementType.DISPENSE, -10);
        movement(medicineId, batchId, actorId, FROM.plusSeconds(180), StockMovementType.RETURN, 3);
        movement(medicineId, batchId, actorId, FROM.plusSeconds(240), StockMovementType.ADJUSTMENT, -2);

        entityManager.flush();
        entityManager.clear();

        List<InventoryStockMovementSummary> summaries = adapter().summarizeMovements(FROM, TO);

        assertEquals(1, summaries.size());
        InventoryStockMovementSummary summary = summaries.get(0);
        assertEquals(medicineId, summary.medicineId());
        assertEquals(45, summary.openingQuantity());
        assertEquals(20, summary.receivedQuantity());
        assertEquals(10, summary.dispensedQuantity());
        assertEquals(3, summary.returnedQuantity());
        assertEquals(-2, summary.adjustedQuantity());
    }

    @Test
    void movementExactlyAtStartBoundaryCountsAsPeriodNotOpening() {
        UUID medicineId = UUID.randomUUID();
        UUID batchId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();

        movement(medicineId, batchId, actorId, FROM, StockMovementType.DISPENSE, -7);

        entityManager.flush();
        entityManager.clear();

        List<InventoryStockMovementSummary> summaries = adapter().summarizeMovements(FROM, TO);

        assertEquals(1, summaries.size());
        assertEquals(0, summaries.get(0).openingQuantity());
        assertEquals(7, summaries.get(0).dispensedQuantity());
    }

    @Test
    void receiptAndMatchingReceiptMovementDoNotDoubleCountOpening() {
        UUID medicineId = UUID.randomUUID();
        UUID batchId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();

        receipt(medicineId, batchId, actorId, FROM.minusSeconds(60), 100);
        receiptMovement(medicineId, batchId, actorId, FROM.minusSeconds(60), 100);

        entityManager.flush();
        entityManager.clear();

        List<InventoryStockMovementSummary> summaries = adapter().summarizeMovements(FROM, TO);

        assertEquals(1, summaries.size());
        assertEquals(100, summaries.get(0).openingQuantity());
        assertEquals(0, summaries.get(0).receivedQuantity());
    }

    @Test
    void expireMovementContributesToAdjustedQuantity() {
        UUID medicineId = UUID.randomUUID();
        UUID batchId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();

        movement(medicineId, batchId, actorId, FROM.plusSeconds(120), StockMovementType.EXPIRE, -10);

        entityManager.flush();
        entityManager.clear();

        List<InventoryStockMovementSummary> summaries = adapter().summarizeMovements(FROM, TO);

        assertEquals(1, summaries.size());
        assertEquals(-10, summaries.get(0).adjustedQuantity());
    }

    @Test
    void futureTransactionsAreExcludedFromReport() {
        UUID medicineId = UUID.randomUUID();
        UUID batchId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();

        receipt(medicineId, batchId, actorId, TO.plusSeconds(60), 10);
        movement(medicineId, batchId, actorId, TO.plusSeconds(60), StockMovementType.DISPENSE, -5);

        entityManager.flush();
        entityManager.clear();

        List<InventoryStockMovementSummary> summaries = adapter().summarizeMovements(FROM, TO);

        assertTrue(summaries.isEmpty());
    }

    @Test
    void returnsEmptyListWhenNoMovementsExist() {
        List<InventoryStockMovementSummary> summaries = adapter().summarizeMovements(FROM, TO);
        assertTrue(summaries.isEmpty());
    }

    private InventoryStockReportRepositoryAdapter adapter() {
        return new InventoryStockReportRepositoryAdapter(entityManager);
    }

    private void receiptMovement(UUID medicineId, UUID batchId, UUID actorId, Instant performedAt, int quantityChange) {
        entityManager.persist(StockMovementEntity.builder()
                .id(UUID.randomUUID())
                .medicineId(medicineId)
                .medicineBatchId(batchId)
                .movementType(StockMovementType.RECEIPT)
                .referenceType(StockMovementReferenceType.INVENTORY_RECEIPT)
                .referenceId(UUID.randomUUID())
                .quantityChange(quantityChange)
                .quantityBefore(0)
                .quantityAfter(quantityChange)
                .performedBy(actorId)
                .performedAt(performedAt)
                .note(null)
                .createdAt(performedAt)
                .build());
    }

    private void receipt(UUID medicineId, UUID batchId, UUID actorId, Instant receivedAt, int quantity) {
        UUID receiptId = UUID.randomUUID();
        entityManager.persist(InventoryReceiptEntity.builder()
                .id(receiptId)
                .receivedBy(actorId)
                .receivedAt(receivedAt)
                .note(null)
                .createdAt(receivedAt)
                .build());
        entityManager.persist(InventoryReceiptItemEntity.builder()
                .id(UUID.randomUUID())
                .inventoryReceiptId(receiptId)
                .medicineId(medicineId)
                .medicineBatchId(batchId)
                .quantity(quantity)
                .importPrice(BigDecimal.ZERO)
                .totalValue(BigDecimal.ZERO)
                .createdAt(receivedAt)
                .build());
    }

    private void movement(
            UUID medicineId,
            UUID batchId,
            UUID actorId,
            Instant performedAt,
            StockMovementType type,
            int quantityChange
    ) {
        int before = Math.max(0, quantityChange < 0 ? -quantityChange : 0);
        entityManager.persist(StockMovementEntity.builder()
                .id(UUID.randomUUID())
                .medicineId(medicineId)
                .medicineBatchId(batchId)
                .movementType(type)
                .referenceType(StockMovementReferenceType.PRESCRIPTION_ITEM)
                .referenceId(UUID.randomUUID())
                .quantityChange(quantityChange)
                .quantityBefore(before)
                .quantityAfter(before + quantityChange)
                .performedBy(actorId)
                .performedAt(performedAt)
                .note(null)
                .createdAt(performedAt)
                .build());
    }
}

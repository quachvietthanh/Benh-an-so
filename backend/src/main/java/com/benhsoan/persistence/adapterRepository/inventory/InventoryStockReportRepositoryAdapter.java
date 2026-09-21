package com.benhsoan.persistence.adapterRepository.inventory;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.domain.inventory.enums.StockMovementType;
import com.benhsoan.port.outbound.repository.inventory.InventoryStockMovementSummary;
import com.benhsoan.port.outbound.repository.inventory.InventoryStockReportRepository;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InventoryStockReportRepositoryAdapter implements InventoryStockReportRepository {

    private final EntityManager entityManager;

    @Override
    public List<InventoryStockMovementSummary> summarizeMovements(
            Instant fromInclusive,
            Instant toExclusive
    ) {
        Map<UUID, MutableSummary> byMedicine = new LinkedHashMap<>();

        for (Object[] row : queryReceiptSummaries(fromInclusive, toExclusive)) {
            MutableSummary summary = byMedicine.computeIfAbsent((UUID) row[0], MutableSummary::new);
            summary.opening += intValue(row[1]);
            summary.received += intValue(row[2]);
        }

        for (Object[] row : queryMovementSummaries(fromInclusive, toExclusive)) {
            MutableSummary summary = byMedicine.computeIfAbsent((UUID) row[0], MutableSummary::new);
            summary.opening += intValue(row[1]);
            summary.dispensed += -intValue(row[2]);
            summary.returned += intValue(row[3]);
            summary.adjusted += intValue(row[4]);
        }

        List<InventoryStockMovementSummary> result = new ArrayList<>(byMedicine.size());
        for (MutableSummary summary : byMedicine.values()) {
            result.add(new InventoryStockMovementSummary(
                    summary.medicineId,
                    summary.opening,
                    summary.received,
                    summary.dispensed,
                    summary.returned,
                    summary.adjusted
            ));
        }
        return result;
    }

    private List<Object[]> queryReceiptSummaries(Instant fromInclusive, Instant toExclusive) {
        return entityManager.createQuery("""
                        select iri.medicineId,
                               sum(case when ir.receivedAt < :fromInclusive then iri.quantity else 0 end),
                               sum(case when ir.receivedAt >= :fromInclusive and ir.receivedAt < :toExclusive then iri.quantity else 0 end)
                        from InventoryReceiptItemEntity iri
                        join InventoryReceiptEntity ir on ir.id = iri.inventoryReceiptId
                        where ir.receivedAt < :toExclusive
                        group by iri.medicineId
                        """, Object[].class)
                .setParameter("fromInclusive", fromInclusive)
                .setParameter("toExclusive", toExclusive)
                .getResultList();
    }

    private List<Object[]> queryMovementSummaries(Instant fromInclusive, Instant toExclusive) {
        return entityManager.createQuery("""
                        select sm.medicineId,
                               sum(case when sm.performedAt < :fromInclusive and sm.movementType <> :receipt then sm.quantityChange else 0 end),
                               sum(case when sm.performedAt >= :fromInclusive and sm.performedAt < :toExclusive and sm.movementType = :dispense then sm.quantityChange else 0 end),
                               sum(case when sm.performedAt >= :fromInclusive and sm.performedAt < :toExclusive and sm.movementType = :return then sm.quantityChange else 0 end),
                               sum(case when sm.performedAt >= :fromInclusive and sm.performedAt < :toExclusive and sm.movementType in (:adjustment, :expire) then sm.quantityChange else 0 end)
                        from StockMovementEntity sm
                        where sm.performedAt < :toExclusive
                        group by sm.medicineId
                        """, Object[].class)
                .setParameter("fromInclusive", fromInclusive)
                .setParameter("toExclusive", toExclusive)
                .setParameter("receipt", StockMovementType.RECEIPT)
                .setParameter("dispense", StockMovementType.DISPENSE)
                .setParameter("return", StockMovementType.RETURN)
                .setParameter("adjustment", StockMovementType.ADJUSTMENT)
                .setParameter("expire", StockMovementType.EXPIRE)
                .getResultList();
    }

    private static int intValue(Object value) {
        return value == null ? 0 : ((Number) value).intValue();
    }

    private static final class MutableSummary {
        private final UUID medicineId;
        private int opening;
        private int received;
        private int dispensed;
        private int returned;
        private int adjusted;

        private MutableSummary(UUID medicineId) {
            this.medicineId = medicineId;
        }
    }
}

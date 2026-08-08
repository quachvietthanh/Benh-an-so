package com.benhsoan.application.ucservice.inventory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.benhsoan.domain.inventory.MedicineBatch;
import com.benhsoan.domain.inventory.enums.BatchStatus;
import com.benhsoan.port.outbound.repository.inventory.MedicineBatchRepository;

class EligibleStockSnapshotServiceTest {

    private static final Instant NOW = Instant.parse("2026-08-08T00:00:00Z");

    private final MedicineBatchRepository medicineBatchRepository = mock(MedicineBatchRepository.class);
    private final EligibleStockSnapshotService service =
            new EligibleStockSnapshotService(medicineBatchRepository, new LowStockEvaluator());

    @Test
    void snapshotsEligibleStockForEachMedicineUsingSharedRule() {
        UUID firstMedicineId = UUID.randomUUID();
        UUID secondMedicineId = UUID.randomUUID();
        LocalDate today = LocalDate.of(2026, 8, 8);

        when(medicineBatchRepository.findByMedicineId(firstMedicineId)).thenReturn(List.of(
                batch(firstMedicineId, "F1", LocalDate.of(2026, 8, 8), 10, BatchStatus.ACTIVE),
                batch(firstMedicineId, "F2", LocalDate.of(2026, 8, 7), 20, BatchStatus.ACTIVE),
                batch(firstMedicineId, "F3", LocalDate.of(2026, 8, 20), 0, BatchStatus.ACTIVE)
        ));
        when(medicineBatchRepository.findByMedicineId(secondMedicineId)).thenReturn(List.of(
                batch(secondMedicineId, "S1", LocalDate.of(2026, 8, 20), 15, BatchStatus.ACTIVE),
                batch(secondMedicineId, "S2", LocalDate.of(2026, 8, 20), 30, BatchStatus.DEPLETED),
                batch(secondMedicineId, "S3", LocalDate.of(2026, 8, 8), 5, BatchStatus.ACTIVE)
        ));

        Map<UUID, Integer> snapshot = service.snapshotEligibleStockQuantities(
                List.of(firstMedicineId, secondMedicineId),
                today
        );

        assertEquals(10, snapshot.get(firstMedicineId));
        assertEquals(20, snapshot.get(secondMedicineId));
    }

    private MedicineBatch batch(
            UUID medicineId,
            String batchNumber,
            LocalDate expiryDate,
            int quantity,
            BatchStatus status
    ) {
        return MedicineBatch.restore(
                UUID.randomUUID(),
                medicineId,
                batchNumber,
                expiryDate,
                quantity,
                status,
                NOW.minusSeconds(3600),
                null
        );
    }
}

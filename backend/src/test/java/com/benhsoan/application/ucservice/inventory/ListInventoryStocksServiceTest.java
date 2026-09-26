package com.benhsoan.application.ucservice.inventory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.benhsoan.domain.inventory.MedicineBatch;
import com.benhsoan.domain.inventory.enums.BatchStatus;
import com.benhsoan.domain.medicine.Medicine;
import com.benhsoan.domain.medicine.enums.AdministrationRoute;
import com.benhsoan.domain.medicine.enums.DosageForm;
import com.benhsoan.port.dto.query.inventory.ListInventoryStocksQuery;
import com.benhsoan.port.dto.result.InventoryStockResult;
import com.benhsoan.port.outbound.repository.inventory.MedicineBatchRepository;
import com.benhsoan.port.outbound.repository.medicine.MedicineRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.benhsoan.port.outbound.time.ClockPort;

class ListInventoryStocksServiceTest {

    private static final Instant NOW = Instant.parse("2026-08-08T00:00:00Z");

    private final MedicineRepository medicineRepository = mock(MedicineRepository.class);
    private final MedicineBatchRepository medicineBatchRepository = mock(MedicineBatchRepository.class);
    private final CurrentUserPort currentUserPort = mock(CurrentUserPort.class);
    private final ClockPort clockPort = mock(ClockPort.class);
    private final InventoryManagementAuthorizer authorizer = new InventoryManagementAuthorizer(currentUserPort);

    private ListInventoryStocksService service;

    @BeforeEach
    void setUp() {
        when(currentUserPort.hasRole("PHARMACIST")).thenReturn(true);
        when(currentUserPort.hasRole("ADMIN")).thenReturn(false);
        when(clockPort.now()).thenReturn(NOW);

        service = new ListInventoryStocksService(
                medicineRepository,
                medicineBatchRepository,
                authorizer,
                clockPort
        );
    }

    @Test
    void excludesExpiredBatchesFromActiveBatchCountAndNearestExpiryDate() {
        UUID medicineId = UUID.randomUUID();
        when(medicineRepository.findAll()).thenReturn(List.of(medicine(medicineId)));
        when(medicineBatchRepository.findAll()).thenReturn(List.of(
                batch(medicineId, "B-EXPIRED", LocalDate.of(2026, 8, 7), 10, BatchStatus.ACTIVE),
                batch(medicineId, "B-ELIGIBLE-1", LocalDate.of(2026, 8, 10), 20, BatchStatus.ACTIVE),
                batch(medicineId, "B-ELIGIBLE-2", LocalDate.of(2026, 8, 9), 15, BatchStatus.ACTIVE),
                batch(medicineId, "B-DEPLETED", LocalDate.of(2026, 8, 12), 0, BatchStatus.DEPLETED)
        ));

        List<InventoryStockResult> results = service.list(new ListInventoryStocksQuery(null));

        assertEquals(1, results.size());
        assertEquals(35, results.getFirst().eligibleStockQuantity());
        assertEquals(2, results.getFirst().activeBatchCount());
        assertEquals(LocalDate.of(2026, 8, 9), results.getFirst().nearestExpiryDate());
    }

    private Medicine medicine(UUID medicineId) {
        return Medicine.restore(
                medicineId,
                "MED-001",
                "Paracetamol",
                "Paracetamol",
                "500 mg",
                DosageForm.TABLET,
                "vien",
                AdministrationRoute.ORAL,
                true,
                NOW.minusSeconds(3600),
                null,
                45,
                20
        );
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
                NOW.minusSeconds(7200),
                null
        );
    }
}

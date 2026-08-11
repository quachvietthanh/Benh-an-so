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
import com.benhsoan.port.dto.result.LowStockMedicineResult;
import com.benhsoan.port.outbound.repository.inventory.MedicineBatchRepository;
import com.benhsoan.port.outbound.repository.medicine.MedicineRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.benhsoan.port.outbound.time.ClockPort;

class ListLowStockMedicinesServiceTest {

    private static final Instant NOW = Instant.parse("2026-08-08T00:00:00Z");

    private final MedicineRepository medicineRepository = mock(MedicineRepository.class);
    private final MedicineBatchRepository medicineBatchRepository = mock(MedicineBatchRepository.class);
    private final CurrentUserPort currentUserPort = mock(CurrentUserPort.class);
    private final ClockPort clockPort = mock(ClockPort.class);
    private final InventoryManagementAuthorizer authorizer = new InventoryManagementAuthorizer(currentUserPort);
    private final LowStockEvaluator lowStockEvaluator = new LowStockEvaluator();
    private final EligibleStockSnapshotService eligibleStockSnapshotService =
            new EligibleStockSnapshotService(medicineBatchRepository, lowStockEvaluator);

    private ListLowStockMedicinesService service;

    @BeforeEach
    void setUp() {
        when(currentUserPort.hasRole("PHARMACIST")).thenReturn(true);
        when(currentUserPort.hasRole("ADMIN")).thenReturn(false);
        when(clockPort.now()).thenReturn(NOW);

        service = new ListLowStockMedicinesService(
                medicineRepository,
                authorizer,
                clockPort,
                eligibleStockSnapshotService,
                lowStockEvaluator
        );
    }

    @Test
    void returnsOnlyMedicinesBelowThreshold() {
        UUID lowMedicineId = UUID.randomUUID();
        UUID okMedicineId = UUID.randomUUID();

        when(medicineRepository.findAllActive()).thenReturn(List.of(
                medicine(lowMedicineId, "MED-LOW", "Metformin", 40, 100),
                medicine(okMedicineId, "MED-OK", "Amlodipine", 20, 100)
        ));
        when(medicineBatchRepository.findByMedicineId(lowMedicineId)).thenReturn(List.of(
                batch(lowMedicineId, "L1", LocalDate.of(2026, 8, 20), 15)
        ));
        when(medicineBatchRepository.findByMedicineId(okMedicineId)).thenReturn(List.of(
                batch(okMedicineId, "O1", LocalDate.of(2026, 8, 20), 25)
        ));

        List<LowStockMedicineResult> results = service.list();

        assertEquals(1, results.size());
        assertEquals(lowMedicineId, results.get(0).medicineId());
        assertEquals(15, results.get(0).eligibleStockQuantity());
        assertEquals(40, results.get(0).minStockThreshold());
        assertEquals(25, results.get(0).shortageQuantity());
    }

    private Medicine medicine(UUID id, String code, String name, int minStockThreshold, int stockQuantity) {
        return Medicine.restore(
                id,
                code,
                name,
                name,
                "500 mg",
                DosageForm.TABLET,
                "viên",
                AdministrationRoute.ORAL,
                true,
                NOW.minusSeconds(3600),
                null,
                stockQuantity,
                minStockThreshold
        );
    }

    private MedicineBatch batch(UUID medicineId, String batchNumber, LocalDate expiryDate, int quantity) {
        return MedicineBatch.restore(
                UUID.randomUUID(),
                medicineId,
                batchNumber,
                expiryDate,
                quantity,
                BatchStatus.ACTIVE,
                NOW.minusSeconds(7200),
                null
        );
    }
}

package com.benhsoan.application.ucservice.inventory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.benhsoan.domain.inventory.MedicineBatch;
import com.benhsoan.domain.inventory.enums.BatchStatus;
import com.benhsoan.domain.inventory.enums.InventoryExpiryAlertStatus;
import com.benhsoan.domain.medicine.Medicine;
import com.benhsoan.domain.medicine.enums.AdministrationRoute;
import com.benhsoan.domain.medicine.enums.DosageForm;
import com.benhsoan.port.dto.query.inventory.ListInventoryExpiryAlertsQuery;
import com.benhsoan.port.dto.result.InventoryExpiryAlertResult;
import com.benhsoan.port.outbound.repository.inventory.MedicineBatchRepository;
import com.benhsoan.port.outbound.repository.medicine.MedicineRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.benhsoan.port.outbound.time.ClockPort;

class ListInventoryExpiryAlertsServiceTest {

    private static final Instant NOW = Instant.parse("2026-08-11T00:00:00Z");

    private final MedicineBatchRepository medicineBatchRepository = mock(MedicineBatchRepository.class);
    private final MedicineRepository medicineRepository = mock(MedicineRepository.class);
    private final CurrentUserPort currentUserPort = mock(CurrentUserPort.class);
    private final ClockPort clockPort = mock(ClockPort.class);
    private final InventoryManagementAuthorizer authorizer = new InventoryManagementAuthorizer(currentUserPort);

    private ListInventoryExpiryAlertsService service;

    @BeforeEach
    void setUp() throws Exception {
        when(currentUserPort.hasRole("PHARMACIST")).thenReturn(true);
        when(currentUserPort.hasRole("ADMIN")).thenReturn(false);
        when(clockPort.now()).thenReturn(NOW);

        service = new ListInventoryExpiryAlertsService(
                medicineBatchRepository,
                medicineRepository,
                authorizer,
                clockPort
        );

        Field field = ListInventoryExpiryAlertsService.class.getDeclaredField("expiryAlertDays");
        field.setAccessible(true);
        field.set(service, 30L);
    }

    @Test
    void returnsNearExpiryAndExpiredBatchesForAllStatus() {
        UUID medicineId = UUID.randomUUID();
        when(medicineBatchRepository.findAll()).thenReturn(List.of(
                batch(medicineId, "B-EXPIRED", LocalDate.of(2026, 8, 10), 12, BatchStatus.ACTIVE),
                batch(medicineId, "B-TODAY", LocalDate.of(2026, 8, 11), 5, BatchStatus.ACTIVE),
                batch(medicineId, "B-NEAR", LocalDate.of(2026, 8, 30), 9, BatchStatus.ACTIVE),
                batch(medicineId, "B-FAR", LocalDate.of(2026, 10, 1), 20, BatchStatus.ACTIVE),
                batch(medicineId, "B-DEPLETED", LocalDate.of(2026, 8, 20), 0, BatchStatus.DEPLETED)
        ));
        when(medicineRepository.findAllById(List.of(medicineId)))
                .thenReturn(List.of(medicine(medicineId, "MED-001", "Paracetamol")));

        List<InventoryExpiryAlertResult> results = service.list(new ListInventoryExpiryAlertsQuery(null, InventoryExpiryAlertStatus.ALL));

        assertEquals(3, results.size());
        assertEquals("B-EXPIRED", results.get(0).batchNumber());
        assertEquals(InventoryExpiryAlertStatus.EXPIRED, results.get(0).alertStatus());
        assertEquals(-1, results.get(0).daysToExpiry());
        assertEquals("B-TODAY", results.get(1).batchNumber());
        assertEquals(InventoryExpiryAlertStatus.NEAR_EXPIRY, results.get(1).alertStatus());
        assertEquals(0, results.get(1).daysToExpiry());
        assertEquals("B-NEAR", results.get(2).batchNumber());
        assertEquals(InventoryExpiryAlertStatus.NEAR_EXPIRY, results.get(2).alertStatus());
        assertEquals(19, results.get(2).daysToExpiry());
    }

    @Test
    void filtersByExpiredStatus() {
        UUID medicineId = UUID.randomUUID();
        when(medicineBatchRepository.findByMedicineId(medicineId)).thenReturn(List.of(
                batch(medicineId, "B-EXPIRED", LocalDate.of(2026, 8, 9), 7, BatchStatus.ACTIVE),
                batch(medicineId, "B-NEAR", LocalDate.of(2026, 8, 15), 7, BatchStatus.ACTIVE)
        ));
        when(medicineRepository.findAllById(List.of(medicineId)))
                .thenReturn(List.of(medicine(medicineId, "MED-002", "Ibuprofen")));

        List<InventoryExpiryAlertResult> results = service.list(
                new ListInventoryExpiryAlertsQuery(medicineId, InventoryExpiryAlertStatus.EXPIRED)
        );

        assertEquals(1, results.size());
        assertEquals("B-EXPIRED", results.get(0).batchNumber());
        assertEquals(InventoryExpiryAlertStatus.EXPIRED, results.get(0).alertStatus());
    }

    @Test
    void returnsEmptyListWhenNoBatchNeedsAlert() {
        UUID medicineId = UUID.randomUUID();
        when(medicineBatchRepository.findAll()).thenReturn(List.of(
                batch(medicineId, "B-FAR", LocalDate.of(2026, 12, 1), 10, BatchStatus.ACTIVE)
        ));
        when(medicineRepository.findAllById(List.of(medicineId)))
                .thenReturn(List.of(medicine(medicineId, "MED-003", "Omeprazole")));

        List<InventoryExpiryAlertResult> results = service.list(null);

        assertTrue(results.isEmpty());
    }

    private MedicineBatch batch(UUID medicineId, String batchNumber, LocalDate expiryDate, int quantity, BatchStatus status) {
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

    private Medicine medicine(UUID id, String code, String name) {
        return Medicine.restore(
                id,
                code,
                name,
                name,
                "500 mg",
                DosageForm.TABLET,
                "vien",
                AdministrationRoute.ORAL,
                true,
                NOW.minusSeconds(7200),
                null,
                100,
                20
        );
    }
}

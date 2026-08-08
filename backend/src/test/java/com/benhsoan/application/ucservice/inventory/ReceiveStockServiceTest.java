package com.benhsoan.application.ucservice.inventory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

import com.benhsoan.domain.inventory.MedicineBatch;
import com.benhsoan.domain.inventory.enums.BatchStatus;
import com.benhsoan.domain.medicine.Medicine;
import com.benhsoan.domain.medicine.enums.AdministrationRoute;
import com.benhsoan.domain.medicine.enums.DosageForm;
import com.benhsoan.domain.shared.exception.ValidationException;
import com.benhsoan.port.dto.command.inventory.ReceiveStockCommand;
import com.benhsoan.port.dto.command.inventory.ReceiveStockItemCommand;
import com.benhsoan.port.dto.result.InventoryReceiptResult;
import com.benhsoan.port.outbound.repository.inventory.InventoryReceiptRepository;
import com.benhsoan.port.outbound.repository.inventory.MedicineBatchRepository;
import com.benhsoan.port.outbound.repository.medicine.MedicineRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.benhsoan.port.outbound.time.ClockPort;

class ReceiveStockServiceTest {

    private static final Instant NOW = Instant.parse("2026-08-07T02:00:00Z");
    private static final UUID USER_ID = UUID.randomUUID();

    private final MedicineRepository medicineRepository = mock(MedicineRepository.class);
    private final MedicineBatchRepository medicineBatchRepository = mock(MedicineBatchRepository.class);
    private final InventoryReceiptRepository inventoryReceiptRepository = mock(InventoryReceiptRepository.class);
    private final CurrentUserPort currentUserPort = mock(CurrentUserPort.class);
    private final ClockPort clockPort = mock(ClockPort.class);
    private final LowStockAlertTransitionService lowStockAlertTransitionService = mock(LowStockAlertTransitionService.class);
    private final InventoryManagementAuthorizer authorizer =
            new InventoryManagementAuthorizer(currentUserPort);
    private final InventoryReceiptResultMapper resultMapper = new InventoryReceiptResultMapper();
    private final EligibleStockSnapshotService eligibleStockSnapshotService = mock(EligibleStockSnapshotService.class);

    private ReceiveStockService service;

    @BeforeEach
    void setUp() {
        when(clockPort.now()).thenReturn(NOW);
        when(currentUserPort.getCurrentUserId()).thenReturn(USER_ID);
        when(medicineBatchRepository.save(any(MedicineBatch.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service = new ReceiveStockService(
                medicineRepository,
                medicineBatchRepository,
                inventoryReceiptRepository,
                authorizer,
                resultMapper,
                eligibleStockSnapshotService,
                lowStockAlertTransitionService,
                currentUserPort,
                clockPort
        );
    }

    @Test
    void successfullyReceivesStockForPharmacist() {
        when(currentUserPort.hasRole("PHARMACIST")).thenReturn(true);
        when(currentUserPort.hasRole("ADMIN")).thenReturn(false);

        UUID medicineId = UUID.randomUUID();
        Medicine medicine = createMedicine(medicineId, "MED-001", "Paracetamol");
        when(medicineRepository.findById(medicineId)).thenReturn(Optional.of(medicine));
        when(medicineBatchRepository.findByMedicineId(medicineId)).thenReturn(List.of());
        when(eligibleStockSnapshotService.snapshotEligibleStockQuantities(List.of(medicineId), LocalDate.of(2026, 8, 7)))
                .thenReturn(java.util.Map.of(medicineId, 0));

        when(medicineBatchRepository.findByMedicineIdAndBatchNumber(eq(medicineId), anyString()))
                .thenReturn(Optional.empty());

        ReceiveStockItemCommand item = new ReceiveStockItemCommand(
                medicineId,
                "BATCH-001",
                LocalDate.of(2027, 12, 31),
                100,
                BigDecimal.valueOf(5.50)
        );

        ReceiveStockCommand command = new ReceiveStockCommand("Test receipt", List.of(item));

        InventoryReceiptResult result = service.receiveStock(command);

        assertNotNull(result);
        assertEquals(USER_ID, result.receivedBy());
        assertEquals("Test receipt", result.note());
        assertEquals(1, result.items().size());
        assertEquals(medicineId, result.items().get(0).medicineId());
        assertEquals(100, result.items().get(0).quantity());
        assertEquals(BigDecimal.valueOf(5.50), result.items().get(0).importPrice());
        assertEquals(0, BigDecimal.valueOf(550.0).compareTo(result.items().get(0).totalValue()));

        verify(medicineRepository).updateStockQuantity(medicineId, 100);
        verify(inventoryReceiptRepository).save(any());
        verify(lowStockAlertTransitionService).handleEligibleStockTransitions(any(), any(), eq(LocalDate.of(2026, 8, 7)), eq(NOW));
    }

    @Test
    void successfullyReceivesStockForAdmin() {
        when(currentUserPort.hasRole("PHARMACIST")).thenReturn(false);
        when(currentUserPort.hasRole("ADMIN")).thenReturn(true);

        UUID medicineId = UUID.randomUUID();
        Medicine medicine = createMedicine(medicineId, "MED-002", "Ibuprofen");
        when(medicineRepository.findById(medicineId)).thenReturn(Optional.of(medicine));
        when(medicineBatchRepository.findByMedicineId(medicineId)).thenReturn(List.of());
        when(eligibleStockSnapshotService.snapshotEligibleStockQuantities(List.of(medicineId), LocalDate.of(2026, 8, 7)))
                .thenReturn(java.util.Map.of(medicineId, 0));

        when(medicineBatchRepository.findByMedicineIdAndBatchNumber(eq(medicineId), anyString()))
                .thenReturn(Optional.empty());

        ReceiveStockItemCommand item = new ReceiveStockItemCommand(
                medicineId, "BATCH-002", LocalDate.of(2028, 6, 15), 50, BigDecimal.valueOf(10.00));
        ReceiveStockCommand command = new ReceiveStockCommand(null, List.of(item));

        InventoryReceiptResult result = service.receiveStock(command);
        assertNotNull(result);
        assertEquals(1, result.items().size());
    }


    @Test
    void rejectsReceiptForUnauthorizedRole() {
        when(currentUserPort.hasRole("PHARMACIST")).thenReturn(false);
        when(currentUserPort.hasRole("ADMIN")).thenReturn(false);

        UUID medicineId = UUID.randomUUID();
        when(medicineRepository.findById(medicineId)).thenReturn(Optional.of(createMedicine(medicineId, "MED-003", "Aspirin")));

        ReceiveStockItemCommand item = new ReceiveStockItemCommand(
                medicineId, "BATCH-003", LocalDate.of(2027, 1, 1), 10, BigDecimal.valueOf(1.00));
        ReceiveStockCommand command = new ReceiveStockCommand(null, List.of(item));

        assertThrows(AccessDeniedException.class, () -> service.receiveStock(command));
    }

    @Test
    void rejectsZeroOrNegativeQuantity() {
        when(currentUserPort.hasRole("PHARMACIST")).thenReturn(true);

        ReceiveStockItemCommand item = new ReceiveStockItemCommand(
                UUID.randomUUID(), "BATCH-004", LocalDate.of(2027, 12, 31), 0, BigDecimal.valueOf(5.00));
        ReceiveStockCommand command = new ReceiveStockCommand(null, List.of(item));

        assertThrows(ValidationException.class, () -> service.receiveStock(command));
    }

    @Test
    void rejectsNegativeImportPrice() {
        when(currentUserPort.hasRole("PHARMACIST")).thenReturn(true);

        UUID medicineId = UUID.randomUUID();
        when(medicineRepository.findById(medicineId)).thenReturn(Optional.of(createMedicine(medicineId, "MED-004", "Cetirizine")));

        ReceiveStockItemCommand item = new ReceiveStockItemCommand(
                medicineId, "BATCH-005", LocalDate.of(2027, 12, 31), 10, BigDecimal.valueOf(-1.00));
        ReceiveStockCommand command = new ReceiveStockCommand(null, List.of(item));

        assertThrows(ValidationException.class, () -> service.receiveStock(command));
    }

    @Test
    void rejectsPastExpiryDate() {
        when(currentUserPort.hasRole("PHARMACIST")).thenReturn(true);

        UUID medicineId = UUID.randomUUID();
        when(medicineRepository.findById(medicineId)).thenReturn(Optional.of(createMedicine(medicineId, "MED-005", "Omeprazole")));

        ReceiveStockItemCommand item = new ReceiveStockItemCommand(
                medicineId, "BATCH-006", LocalDate.of(2020, 1, 1), 10, BigDecimal.valueOf(5.00));
        ReceiveStockCommand command = new ReceiveStockCommand(null, List.of(item));

        assertThrows(ValidationException.class, () -> service.receiveStock(command));
    }

    @Test
    void rejectsTodayExpiryDate() {
        when(currentUserPort.hasRole("PHARMACIST")).thenReturn(true);

        UUID medicineId = UUID.randomUUID();
        when(medicineRepository.findById(medicineId)).thenReturn(Optional.of(createMedicine(medicineId, "MED-006", "Metformin")));

        ReceiveStockItemCommand item = new ReceiveStockItemCommand(
                medicineId, "BATCH-007", LocalDate.of(2026, 8, 7), 10, BigDecimal.valueOf(5.00));
        ReceiveStockCommand command = new ReceiveStockCommand(null, List.of(item));

        assertThrows(ValidationException.class, () -> service.receiveStock(command));
    }


    @Test
    void rejectsInvalidMedicineId() {
        when(currentUserPort.hasRole("PHARMACIST")).thenReturn(true);

        UUID medicineId = UUID.randomUUID();
        when(medicineRepository.findById(medicineId)).thenReturn(Optional.empty());

        ReceiveStockItemCommand item = new ReceiveStockItemCommand(
                medicineId, "BATCH-008", LocalDate.of(2027, 12, 31), 10, BigDecimal.valueOf(5.00));
        ReceiveStockCommand command = new ReceiveStockCommand(null, List.of(item));

        assertThrows(ValidationException.class, () -> service.receiveStock(command));
    }

    @Test
    void rejectsNullCommand() {
        when(currentUserPort.hasRole("PHARMACIST")).thenReturn(true);
        assertThrows(ValidationException.class, () -> service.receiveStock(null));
    }

    @Test
    void rejectsEmptyItems() {
        when(currentUserPort.hasRole("PHARMACIST")).thenReturn(true);
        ReceiveStockCommand command = new ReceiveStockCommand(null, List.of());
        assertThrows(ValidationException.class, () -> service.receiveStock(command));
    }

    @Test
    void addsToExistingBatch() {
        when(currentUserPort.hasRole("PHARMACIST")).thenReturn(true);
        when(currentUserPort.hasRole("ADMIN")).thenReturn(false);

        UUID medicineId = UUID.randomUUID();
        when(medicineRepository.findById(medicineId)).thenReturn(Optional.of(createMedicine(medicineId, "MED-010", "Amoxicillin")));

        UUID batchId = UUID.randomUUID();
        MedicineBatch existingBatch = MedicineBatch.restore(
                batchId, medicineId, "BATCH-EXISTING",
                LocalDate.of(2027, 12, 31), 50, BatchStatus.ACTIVE,
                NOW.minusSeconds(3600), null);
        when(medicineBatchRepository.findByMedicineId(medicineId)).thenReturn(List.of(existingBatch));
        when(eligibleStockSnapshotService.snapshotEligibleStockQuantities(List.of(medicineId), LocalDate.of(2026, 8, 7)))
                .thenReturn(java.util.Map.of(medicineId, 50));
        when(medicineBatchRepository.findByMedicineIdAndBatchNumber(eq(medicineId), eq("BATCH-EXISTING")))
                .thenReturn(Optional.of(existingBatch));

        ReceiveStockItemCommand item = new ReceiveStockItemCommand(
                medicineId, "BATCH-EXISTING", LocalDate.of(2027, 12, 31), 30, BigDecimal.valueOf(5.00));
        ReceiveStockCommand command = new ReceiveStockCommand("Add to existing batch", List.of(item));

        InventoryReceiptResult result = service.receiveStock(command);

        assertNotNull(result);
        assertEquals(1, result.items().size());
        assertEquals(30, result.items().get(0).quantity());
        verify(medicineBatchRepository).addStockQuantity(batchId, 30);
        verify(medicineRepository).updateStockQuantity(medicineId, 30);
        verify(lowStockAlertTransitionService).handleEligibleStockTransitions(any(), any(), eq(LocalDate.of(2026, 8, 7)), eq(NOW));
    }

    @Test
    void shouldCalculateAfterEligibleStockFromActualBatchState() {
        when(currentUserPort.hasRole("PHARMACIST")).thenReturn(true);
        when(currentUserPort.hasRole("ADMIN")).thenReturn(false);

        UUID medicineId = UUID.randomUUID();
        when(medicineRepository.findById(medicineId)).thenReturn(Optional.of(createMedicine(medicineId, "MED-011", "Loratadine")));

        MedicineBatch expiredBatch = MedicineBatch.restore(
                UUID.randomUUID(),
                medicineId,
                "BATCH-EXPIRED",
                LocalDate.of(2026, 8, 1),
                10,
                BatchStatus.ACTIVE,
                NOW.minusSeconds(7200),
                null
        );
        when(medicineBatchRepository.findByMedicineId(medicineId)).thenReturn(List.of(expiredBatch));
        when(eligibleStockSnapshotService.snapshotEligibleStockQuantities(List.of(medicineId), LocalDate.of(2026, 8, 7)))
                .thenReturn(java.util.Map.of(medicineId, 0));
        when(medicineBatchRepository.findByMedicineIdAndBatchNumber(eq(medicineId), eq("BATCH-NEW")))
                .thenReturn(Optional.empty());

        ReceiveStockItemCommand item = new ReceiveStockItemCommand(
                medicineId,
                "BATCH-NEW",
                LocalDate.of(2027, 12, 31),
                30,
                BigDecimal.valueOf(7.00)
        );

        service.receiveStock(new ReceiveStockCommand("Receipt with expired stock present", List.of(item)));

        verify(lowStockAlertTransitionService).handleEligibleStockTransitions(
                argThat(ids -> ids.size() == 1 && ids.contains(medicineId)),
                argThat(before -> before.size() == 1 && Integer.valueOf(0).equals(before.get(medicineId))),
                eq(LocalDate.of(2026, 8, 7)),
                eq(NOW)
        );
    }

    @Test
    void rejectsExistingBatchWithMismatchedExpiryDate() {
        when(currentUserPort.hasRole("PHARMACIST")).thenReturn(true);
        when(currentUserPort.hasRole("ADMIN")).thenReturn(false);

        UUID medicineId = UUID.randomUUID();
        when(medicineRepository.findById(medicineId)).thenReturn(Optional.of(createMedicine(medicineId, "MED-012", "Cefixime")));

        MedicineBatch existingBatch = MedicineBatch.restore(
                UUID.randomUUID(),
                medicineId,
                "BATCH-MISMATCH",
                LocalDate.of(2027, 12, 31),
                50,
                BatchStatus.ACTIVE,
                NOW.minusSeconds(3600),
                null
        );
        when(medicineBatchRepository.findByMedicineId(medicineId)).thenReturn(List.of(existingBatch));
        when(eligibleStockSnapshotService.snapshotEligibleStockQuantities(List.of(medicineId), LocalDate.of(2026, 8, 7)))
                .thenReturn(java.util.Map.of(medicineId, 50));
        when(medicineBatchRepository.findByMedicineIdAndBatchNumber(eq(medicineId), eq("BATCH-MISMATCH")))
                .thenReturn(Optional.of(existingBatch));

        ReceiveStockItemCommand item = new ReceiveStockItemCommand(
                medicineId,
                "BATCH-MISMATCH",
                LocalDate.of(2028, 1, 1),
                10,
                BigDecimal.valueOf(5.00)
        );

        ValidationException ex = assertThrows(
                ValidationException.class,
                () -> service.receiveStock(new ReceiveStockCommand("Mismatch expiry", List.of(item)))
        );

        assertEquals(
                "Batch number must map to a single expiry date for the same medicine."
                        + " medicineId=" + medicineId
                        + ", batchNumber=BATCH-MISMATCH"
                        + ", existingExpiryDate=2027-12-31"
                        + ", requestedExpiryDate=2028-01-01",
                ex.getMessage()
        );
        verify(medicineBatchRepository, never()).addStockQuantity(any(), anyInt());
        verify(lowStockAlertTransitionService, never()).handleEligibleStockTransitions(any(), any(), any(LocalDate.class), any());
    }

    @Test
    void rejectsDuplicateItems() {
        when(currentUserPort.hasRole("PHARMACIST")).thenReturn(true);

        UUID medicineId = UUID.randomUUID();

        ReceiveStockItemCommand item1 = new ReceiveStockItemCommand(
                medicineId, "BATCH-DUP", LocalDate.of(2027, 12, 31), 10, BigDecimal.valueOf(5.00));
        ReceiveStockItemCommand item2 = new ReceiveStockItemCommand(
                medicineId, "BATCH-DUP", LocalDate.of(2027, 12, 31), 20, BigDecimal.valueOf(5.00));
        ReceiveStockCommand command = new ReceiveStockCommand(null, List.of(item1, item2));

        assertThrows(ValidationException.class, () -> service.receiveStock(command));
    }

    private Medicine createMedicine(UUID id, String code, String name) {
        return Medicine.restore(
                id, code, name, name, "500 mg", DosageForm.TABLET, "vien",
                AdministrationRoute.ORAL, true, NOW.minusSeconds(86400), null, 0, 20);
    }
}

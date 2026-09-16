package com.benhsoan.application.ucservice.prescription;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.access.AccessDeniedException;

import com.benhsoan.application.ucservice.inventory.EligibleStockSnapshotService;
import com.benhsoan.application.ucservice.inventory.LowStockAlertTransitionService;
import com.benhsoan.domain.auditlog.AuditLog;
import com.benhsoan.domain.inventory.MedicineBatch;
import com.benhsoan.domain.inventory.enums.BatchStatus;
import com.benhsoan.domain.medicine.Medicine;
import com.benhsoan.domain.medicine.enums.AdministrationRoute;
import com.benhsoan.domain.medicine.enums.DosageForm;
import com.benhsoan.domain.prescription.Prescription;
import com.benhsoan.domain.prescription.PrescriptionDispenseItem;
import com.benhsoan.domain.prescription.PrescriptionItem;
import com.benhsoan.domain.prescription.enums.InterconnectionStatus;
import com.benhsoan.domain.prescription.enums.PrescriptionStatus;
import com.benhsoan.domain.prescription.exception.PrescriptionAlreadyDispensedException;
import com.benhsoan.domain.prescription.exception.PrescriptionInsufficientStockException;
import com.benhsoan.domain.prescription.exception.PrescriptionInvalidStatusException;
import com.benhsoan.domain.shared.exception.ValidationException;
import com.benhsoan.port.dto.command.prescription.DispenseItemCommand;
import com.benhsoan.port.dto.command.prescription.DispensePrescriptionItemsCommand;
import com.benhsoan.port.outbound.repository.audit.AuditLogRepository;
import com.benhsoan.port.outbound.repository.inventory.MedicineBatchRepository;
import com.benhsoan.port.outbound.repository.inventory.StockMovementRepository;
import com.benhsoan.port.outbound.repository.medicine.MedicineRepository;
import com.benhsoan.port.outbound.repository.prescription.PrescriptionDispenseItemRepository;
import com.benhsoan.port.outbound.repository.prescription.PrescriptionRepository;
import com.benhsoan.port.outbound.repository.prescription.PrescriptionWarningLogRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.benhsoan.port.outbound.time.ClockPort;

class PartialDispensePrescriptionServiceTest {

    private static final Instant NOW = Instant.parse("2026-08-07T02:00:00Z");
    private static final UUID ACTOR_ID = UUID.randomUUID();
    private static final UUID MEDICINE_ID = UUID.randomUUID();

    private final PrescriptionRepository prescriptionRepository = mock(PrescriptionRepository.class);
    private final PrescriptionDispenseItemRepository dispenseItemRepository =
            mock(PrescriptionDispenseItemRepository.class);
    private final PrescriptionWarningLogRepository warningLogRepository = mock(PrescriptionWarningLogRepository.class);
    private final MedicineRepository medicineRepository = mock(MedicineRepository.class);
    private final MedicineBatchRepository medicineBatchRepository = mock(MedicineBatchRepository.class);
    private final StockMovementRepository stockMovementRepository = mock(StockMovementRepository.class);
    private final EligibleStockSnapshotService eligibleStockSnapshotService =
            mock(EligibleStockSnapshotService.class);
    private final LowStockAlertTransitionService lowStockAlertTransitionService =
            mock(LowStockAlertTransitionService.class);
    private final CurrentUserPort currentUserPort = mock(CurrentUserPort.class);
    private final ClockPort clockPort = mock(ClockPort.class);
    private final AuditLogRepository auditLogRepository = mock(AuditLogRepository.class);
    private final PartialDispensePrescriptionResultMapper resultMapper =
            mock(PartialDispensePrescriptionResultMapper.class);

    private PartialDispensePrescriptionService service;

    @BeforeEach
    void setUp() {
        service = new PartialDispensePrescriptionService(
                prescriptionRepository,
                dispenseItemRepository,
                warningLogRepository,
                medicineRepository,
                medicineBatchRepository,
                stockMovementRepository,
                eligibleStockSnapshotService,
                lowStockAlertTransitionService,
                currentUserPort,
                clockPort,
                auditLogRepository,
                resultMapper);
        when(currentUserPort.hasRole("PHARMACIST")).thenReturn(true);
        when(currentUserPort.getCurrentUserId()).thenReturn(ACTOR_ID);
        when(clockPort.now()).thenReturn(NOW);
    }

    private void stubPrescription(Prescription prescription) {
        when(prescriptionRepository.findByIdForUpdate(prescription.getId()))
                .thenReturn(Optional.of(prescription));
        when(prescriptionRepository.save(any(Prescription.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    private void stubMedicine() {
        when(medicineRepository.findAllById(any())).thenReturn(List.of(medicine()));
    }

    private void stubBatch(int quantity) {
        when(medicineBatchRepository.findAvailableByMedicineIdForUpdate(eq(MEDICINE_ID), any()))
                .thenReturn(List.of(batch(quantity)));
        when(eligibleStockSnapshotService.snapshotEligibleStockQuantities(any(), any()))
                .thenReturn(Map.of(MEDICINE_ID, quantity));
    }

    private Medicine medicine() {
        return Medicine.restore(
                MEDICINE_ID, "MED-001", "Paracetamol", "Paracetamol", "500 mg",
                DosageForm.TABLET, "vien", AdministrationRoute.ORAL, true,
                NOW.minusSeconds(86400), null, 120, 20);
    }

    private MedicineBatch batch(int quantity) {
        return MedicineBatch.restore(
                UUID.randomUUID(), MEDICINE_ID, "BATCH-A", LocalDate.of(2026, 12, 1),
                quantity, BatchStatus.ACTIVE, NOW.minusSeconds(3600), null);
    }

    private PrescriptionItem item(UUID prescriptionId, UUID itemId, int prescribed, int dispensed) {
        return PrescriptionItem.restore(
                itemId, prescriptionId, MEDICINE_ID, "Paracetamol", "Paracetamol",
                "500 mg", "vien", "1 vien", 2, AdministrationRoute.ORAL, 5,
                prescribed, dispensed, null, NOW.minusSeconds(600), null);
    }

    private Prescription prescription(UUID prescriptionId, PrescriptionStatus status, PrescriptionItem item) {
        return Prescription.restore(
                prescriptionId, "RX-001", UUID.randomUUID(), status, "note",
                null, UUID.randomUUID(), NOW.minusSeconds(600), null, null,
                InterconnectionStatus.NOT_SENT, null, null, null, List.of(item));
    }

    @Test
    void partiallyDispensesWhenStockInsufficient() {
        UUID prescriptionId = UUID.randomUUID();
        UUID itemId = UUID.randomUUID();
        PrescriptionItem item = item(prescriptionId, itemId, 20, 0);
        Prescription prescription = prescription(prescriptionId, PrescriptionStatus.PENDING_DISPENSE, item);
        stubPrescription(prescription);
        stubMedicine();
        stubBatch(12);

        service.dispense(new DispensePrescriptionItemsCommand(prescriptionId,
                List.of(new DispenseItemCommand(itemId, 12))));

        assertEquals(PrescriptionStatus.PARTIALLY_DISPENSED, prescription.getStatus());
        assertEquals(12, item.getDispensedQuantity());
        assertEquals(8, item.getRemainingQuantity());
        verify(medicineBatchRepository)
                .deductStockQuantity(any(), eq(12), eq(BatchStatus.DEPLETED), eq(NOW));
        List<PrescriptionDispenseItem> events = capturedDispenseItems();
        assertEquals(1, events.size());
        assertEquals(12, events.get(0).getDispensedQuantity());
        assertEquals(ACTOR_ID, events.get(0).getDispensedBy());
        assertEquals(MEDICINE_ID, events.get(0).getMedicineId());
    }

    @Test
    void rejectsDispensingMoreThanAvailableInventory() {
        UUID prescriptionId = UUID.randomUUID();
        UUID itemId = UUID.randomUUID();
        PrescriptionItem item = item(prescriptionId, itemId, 20, 0);
        Prescription prescription = prescription(prescriptionId, PrescriptionStatus.PENDING_DISPENSE, item);
        stubPrescription(prescription);
        stubMedicine();
        stubBatch(12);

        assertThrows(PrescriptionInsufficientStockException.class, () -> service.dispense(
                new DispensePrescriptionItemsCommand(prescriptionId,
                        List.of(new DispenseItemCommand(itemId, 15)))));

        verify(medicineBatchRepository, never())
                .deductStockQuantity(any(), org.mockito.ArgumentMatchers.anyInt(), any(), any());
        verify(dispenseItemRepository, never()).saveAll(any());
        verify(prescriptionRepository, never()).save(any());
        assertEquals(PrescriptionStatus.PENDING_DISPENSE, prescription.getStatus());
    }

    @Test
    void completesRemainingAfterReplenishment() {
        UUID prescriptionId = UUID.randomUUID();
        UUID itemId = UUID.randomUUID();
        PrescriptionItem item = item(prescriptionId, itemId, 20, 12);
        Prescription prescription = prescription(prescriptionId, PrescriptionStatus.PARTIALLY_DISPENSED, item);
        stubPrescription(prescription);
        stubMedicine();
        stubBatch(8);

        service.dispense(new DispensePrescriptionItemsCommand(prescriptionId,
                List.of(new DispenseItemCommand(itemId, 8))));

        assertEquals(PrescriptionStatus.DISPENSED, prescription.getStatus());
        assertEquals(20, item.getDispensedQuantity());
        assertEquals(0, item.getRemainingQuantity());
        verify(medicineBatchRepository)
                .deductStockQuantity(any(), eq(8), eq(BatchStatus.DEPLETED), eq(NOW));
        List<PrescriptionDispenseItem> events = capturedDispenseItems();
        assertEquals(1, events.size());
        assertEquals(8, events.get(0).getDispensedQuantity());
    }

    @Test
    void auditLogUsesInjectedClockTimestamp() {
        UUID prescriptionId = UUID.randomUUID();
        UUID itemId = UUID.randomUUID();
        PrescriptionItem item = item(prescriptionId, itemId, 20, 0);
        Prescription prescription = prescription(prescriptionId, PrescriptionStatus.PENDING_DISPENSE, item);
        stubPrescription(prescription);
        stubMedicine();
        stubBatch(12);

        service.dispense(new DispensePrescriptionItemsCommand(prescriptionId,
                List.of(new DispenseItemCommand(itemId, 12))));

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        assertEquals(NOW, captor.getValue().getCreatedAt());
    }

    @Test
    void rejectsZeroOrNegativeQuantity() {
        UUID prescriptionId = UUID.randomUUID();
        UUID itemId = UUID.randomUUID();
        PrescriptionItem item = item(prescriptionId, itemId, 20, 0);
        Prescription prescription = prescription(prescriptionId, PrescriptionStatus.PENDING_DISPENSE, item);
        stubPrescription(prescription);
        stubMedicine();
        stubBatch(12);

        assertThrows(ValidationException.class, () -> service.dispense(
                new DispensePrescriptionItemsCommand(prescriptionId,
                        List.of(new DispenseItemCommand(itemId, 0)))));
        assertThrows(ValidationException.class, () -> service.dispense(
                new DispensePrescriptionItemsCommand(prescriptionId,
                        List.of(new DispenseItemCommand(itemId, -1)))));
    }

    @Test
    void rejectsQuantityExceedingRemaining() {
        UUID prescriptionId = UUID.randomUUID();
        UUID itemId = UUID.randomUUID();
        PrescriptionItem item = item(prescriptionId, itemId, 20, 12);
        Prescription prescription = prescription(prescriptionId, PrescriptionStatus.PARTIALLY_DISPENSED, item);
        stubPrescription(prescription);
        stubMedicine();
        stubBatch(20);

        assertThrows(ValidationException.class, () -> service.dispense(
                new DispensePrescriptionItemsCommand(prescriptionId,
                        List.of(new DispenseItemCommand(itemId, 9)))));
    }

    @Test
    void rejectsUnknownPrescriptionItem() {
        UUID prescriptionId = UUID.randomUUID();
        UUID itemId = UUID.randomUUID();
        PrescriptionItem item = item(prescriptionId, itemId, 20, 0);
        Prescription prescription = prescription(prescriptionId, PrescriptionStatus.PENDING_DISPENSE, item);
        stubPrescription(prescription);
        stubMedicine();
        stubBatch(12);

        assertThrows(ValidationException.class, () -> service.dispense(
                new DispensePrescriptionItemsCommand(prescriptionId,
                        List.of(new DispenseItemCommand(UUID.randomUUID(), 5)))));
    }

    @Test
    void rejectsDuplicatePrescriptionItemIds() {
        UUID prescriptionId = UUID.randomUUID();
        UUID itemId = UUID.randomUUID();
        PrescriptionItem item = item(prescriptionId, itemId, 20, 0);
        Prescription prescription = prescription(prescriptionId, PrescriptionStatus.PENDING_DISPENSE, item);
        stubPrescription(prescription);
        stubMedicine();
        stubBatch(20);

        ValidationException ex = assertThrows(ValidationException.class, () -> service.dispense(
                new DispensePrescriptionItemsCommand(prescriptionId,
                        List.of(
                                new DispenseItemCommand(itemId, 5),
                                new DispenseItemCommand(itemId, 3)))));
        assertTrue(ex.getMessage().contains("Duplicate prescription item"));
    }

    @Test
    void rejectsCancelledPrescription() {
        UUID prescriptionId = UUID.randomUUID();
        UUID itemId = UUID.randomUUID();
        PrescriptionItem item = item(prescriptionId, itemId, 20, 0);
        Prescription prescription = prescription(prescriptionId, PrescriptionStatus.CANCELLED, item);
        stubPrescription(prescription);

        assertThrows(PrescriptionInvalidStatusException.class, () -> service.dispense(
                new DispensePrescriptionItemsCommand(prescriptionId,
                        List.of(new DispenseItemCommand(itemId, 5)))));
    }

    @Test
    void rejectsDispensedPrescription() {
        UUID prescriptionId = UUID.randomUUID();
        UUID itemId = UUID.randomUUID();
        PrescriptionItem item = item(prescriptionId, itemId, 20, 20);
        Prescription prescription = prescription(prescriptionId, PrescriptionStatus.DISPENSED, item);
        stubPrescription(prescription);

        assertThrows(PrescriptionAlreadyDispensedException.class, () -> service.dispense(
                new DispensePrescriptionItemsCommand(prescriptionId,
                        List.of(new DispenseItemCommand(itemId, 5)))));
    }

    @Test
    void rejectsUnauthorizedRole() {
        when(currentUserPort.hasRole("PHARMACIST")).thenReturn(false);
        when(currentUserPort.hasRole("ADMIN")).thenReturn(false);

        assertThrows(AccessDeniedException.class, () -> service.dispense(
                new DispensePrescriptionItemsCommand(UUID.randomUUID(), List.of())));
    }

    @SuppressWarnings("unchecked")
    private List<PrescriptionDispenseItem> capturedDispenseItems() {
        ArgumentCaptor<List<PrescriptionDispenseItem>> captor =
                (ArgumentCaptor) ArgumentCaptor.forClass(List.class);
        verify(dispenseItemRepository).saveAll(captor.capture());
        return captor.getValue();
    }
}

package com.benhsoan.application.ucservice.prescription;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.benhsoan.domain.inventory.MedicineBatch;
import com.benhsoan.domain.inventory.StockMovement;
import com.benhsoan.domain.inventory.enums.BatchStatus;
import com.benhsoan.domain.medicine.Medicine;
import com.benhsoan.domain.medicine.enums.AdministrationRoute;
import com.benhsoan.domain.medicine.enums.DosageForm;
import com.benhsoan.domain.prescription.Prescription;
import com.benhsoan.domain.prescription.PrescriptionDispenseItem;
import com.benhsoan.domain.prescription.PrescriptionItem;
import com.benhsoan.domain.prescription.enums.PrescriptionStatus;
import com.benhsoan.domain.prescription.exception.PrescriptionInsufficientStockException;
import com.benhsoan.domain.shared.exception.ValidationException;
import com.benhsoan.port.dto.result.DispensePrescriptionResult;
import com.benhsoan.port.outbound.repository.audit.AuditLogRepository;
import com.benhsoan.port.outbound.repository.inventory.MedicineBatchRepository;
import com.benhsoan.port.outbound.repository.inventory.StockMovementRepository;
import com.benhsoan.port.outbound.repository.medicine.MedicineRepository;
import com.benhsoan.port.outbound.repository.prescription.PrescriptionDispenseItemRepository;
import com.benhsoan.port.outbound.repository.prescription.PrescriptionItemRepository;
import com.benhsoan.port.outbound.repository.prescription.PrescriptionRepository;
import com.benhsoan.port.outbound.repository.prescription.PrescriptionWarningLogRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.benhsoan.port.outbound.time.ClockPort;

class DispensePrescriptionServiceTest {

    private static final Instant NOW = Instant.parse("2026-08-07T02:00:00Z");
    private static final UUID ACTOR_ID = UUID.randomUUID();

    private final PrescriptionRepository prescriptionRepository = mock(PrescriptionRepository.class);
    private final PrescriptionItemRepository prescriptionItemRepository = mock(PrescriptionItemRepository.class);
    private final PrescriptionDispenseItemRepository prescriptionDispenseItemRepository =
            mock(PrescriptionDispenseItemRepository.class);
    private final PrescriptionWarningLogRepository warningLogRepository = mock(PrescriptionWarningLogRepository.class);
    private final MedicineRepository medicineRepository = mock(MedicineRepository.class);
    private final MedicineBatchRepository medicineBatchRepository = mock(MedicineBatchRepository.class);
    private final StockMovementRepository stockMovementRepository = mock(StockMovementRepository.class);
    private final CurrentUserPort currentUserPort = mock(CurrentUserPort.class);
    private final ClockPort clockPort = mock(ClockPort.class);
    private final AuditLogRepository auditLogRepository = mock(AuditLogRepository.class);
    private final com.benhsoan.application.ucservice.inventory.LowStockAlertTransitionService lowStockAlertTransitionService =
            mock(com.benhsoan.application.ucservice.inventory.LowStockAlertTransitionService.class);
    private final PrescriptionDisplayContextResolver displayContextResolver = mock(PrescriptionDisplayContextResolver.class);
    private final DispensePrescriptionResultMapper dispensePrescriptionResultMapper =
            new DispensePrescriptionResultMapper(new PrescriptionResultMapper(displayContextResolver));

    private DispensePrescriptionService service;

    @BeforeEach
    void setUp() {
        when(currentUserPort.hasRole("PHARMACIST")).thenReturn(true);
        when(currentUserPort.hasRole("ADMIN")).thenReturn(false);
        when(currentUserPort.getCurrentUserId()).thenReturn(ACTOR_ID);
        when(clockPort.now()).thenReturn(NOW);
        when(warningLogRepository.findByPrescriptionId(any())).thenReturn(List.of());
        when(displayContextResolver.resolve(any(), any()))
                .thenReturn(new PrescriptionDisplayContextResolver.PrescriptionDisplayContext(
                        UUID.randomUUID(),
                        "VISIT-001",
                        UUID.randomUUID(),
                        "PAT-001",
                        "Nguyen Van A",
                        "Dr. B"
                ));
        when(prescriptionDispenseItemRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(stockMovementRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service = new DispensePrescriptionService(
                prescriptionRepository,
                prescriptionItemRepository,
                prescriptionDispenseItemRepository,
                warningLogRepository,
                medicineRepository,
                medicineBatchRepository,
                stockMovementRepository,
                lowStockAlertTransitionService,
                currentUserPort,
                clockPort,
                auditLogRepository,
                dispensePrescriptionResultMapper
        );
    }

    @Test
    void shouldDispenseUsingFefoAcrossMultipleBatches() {
        UUID prescriptionId = UUID.randomUUID();
        UUID medicineId = UUID.randomUUID();
        Prescription prescription = prescription(prescriptionId, medicineId);
        PrescriptionItem prescriptionItem = prescriptionItem(prescriptionId, medicineId, 70);

        MedicineBatch firstBatch = batch(medicineId, "BATCH-A", LocalDate.of(2026, 10, 1), 20);
        MedicineBatch secondBatch = batch(medicineId, "BATCH-B", LocalDate.of(2026, 12, 1), 60);

        when(prescriptionRepository.findByIdForUpdate(prescriptionId)).thenReturn(Optional.of(prescription));
        when(prescriptionItemRepository.findByPrescriptionId(prescriptionId)).thenReturn(List.of(prescriptionItem));
        when(medicineRepository.findById(medicineId)).thenReturn(Optional.of(medicine(medicineId, "MED-001", "Paracetamol")));
        when(medicineBatchRepository.findAvailableByMedicineIdForUpdate(eq(medicineId), eq(LocalDate.of(2026, 8, 7))))
                .thenReturn(List.of(firstBatch, secondBatch));
        when(prescriptionRepository.save(any(Prescription.class))).thenAnswer(invocation -> invocation.getArgument(0));

        DispensePrescriptionResult result = service.dispense(prescriptionId);

        assertNotNull(result);
        assertEquals(PrescriptionStatus.DISPENSED, result.prescription().status());
        assertEquals(ACTOR_ID, result.dispensedBy());
        assertEquals(NOW, result.dispensedAt());
        assertEquals(2, result.allocationCount());
        assertEquals(70, result.totalDispensedQuantity());
        assertEquals("BATCH-A", result.allocations().get(0).batchNumber());
        assertEquals(20, result.allocations().get(0).dispensedQuantity());
        assertEquals(0, result.allocations().get(0).batchQuantityRemaining());
        assertEquals("BATCH-B", result.allocations().get(1).batchNumber());
        assertEquals(50, result.allocations().get(1).dispensedQuantity());
        assertEquals(10, result.allocations().get(1).batchQuantityRemaining());
        verify(medicineBatchRepository).deductStockQuantity(firstBatch.getId(), 20, BatchStatus.DEPLETED, NOW);
        verify(medicineBatchRepository).deductStockQuantity(secondBatch.getId(), 50, BatchStatus.ACTIVE, NOW);
        verify(medicineRepository).updateStockQuantity(medicineId, -70);
        verify(prescriptionDispenseItemRepository).saveAll(any());
        verify(stockMovementRepository).saveAll(any());
        verify(auditLogRepository).save(any());
        verify(lowStockAlertTransitionService).handleEligibleStockTransitions(any(), any(), eq(NOW));
    }

    @Test
    void shouldThrowConflictWhenStockIsInsufficient() {
        UUID prescriptionId = UUID.randomUUID();
        UUID medicineId = UUID.randomUUID();
        Prescription prescription = prescription(prescriptionId, medicineId);
        PrescriptionItem prescriptionItem = prescriptionItem(prescriptionId, medicineId, 70);
        MedicineBatch onlyBatch = batch(medicineId, "BATCH-A", LocalDate.of(2026, 10, 1), 20);

        when(prescriptionRepository.findByIdForUpdate(prescriptionId)).thenReturn(Optional.of(prescription));
        when(prescriptionItemRepository.findByPrescriptionId(prescriptionId)).thenReturn(List.of(prescriptionItem));
        when(medicineRepository.findById(medicineId)).thenReturn(Optional.of(medicine(medicineId, "MED-001", "Paracetamol")));
        when(medicineBatchRepository.findAvailableByMedicineIdForUpdate(eq(medicineId), eq(LocalDate.of(2026, 8, 7))))
                .thenReturn(List.of(onlyBatch));

        PrescriptionInsufficientStockException ex = assertThrows(
                PrescriptionInsufficientStockException.class,
                () -> service.dispense(prescriptionId)
        );

        assertEquals(prescriptionId, ex.getPrescriptionId());
        assertEquals(1, ex.getDetails().size());
        assertEquals(50, ex.getDetails().get(0).shortageQuantity());
    }

    @Test
    void shouldTranslateConcurrentDeductionFailureToInsufficientStockConflict() {
        UUID prescriptionId = UUID.randomUUID();
        UUID medicineId = UUID.randomUUID();
        Prescription prescription = prescription(prescriptionId, medicineId);
        PrescriptionItem prescriptionItem = prescriptionItem(prescriptionId, medicineId, 70);

        MedicineBatch firstBatch = batch(medicineId, "BATCH-A", LocalDate.of(2026, 10, 1), 20);
        MedicineBatch secondBatch = batch(medicineId, "BATCH-B", LocalDate.of(2026, 12, 1), 60);

        when(prescriptionRepository.findByIdForUpdate(prescriptionId)).thenReturn(Optional.of(prescription));
        when(prescriptionItemRepository.findByPrescriptionId(prescriptionId)).thenReturn(List.of(prescriptionItem));
        when(medicineRepository.findById(medicineId)).thenReturn(Optional.of(medicine(medicineId, "MED-001", "Paracetamol")));
        when(medicineBatchRepository.findAvailableByMedicineIdForUpdate(eq(medicineId), eq(LocalDate.of(2026, 8, 7))))
                .thenReturn(List.of(firstBatch, secondBatch));
        org.mockito.Mockito.doThrow(new ValidationException("Unable to deduct stock for batch id"))
                .when(medicineBatchRepository)
                .deductStockQuantity(secondBatch.getId(), 50, BatchStatus.ACTIVE, NOW);

        PrescriptionInsufficientStockException ex = assertThrows(
                PrescriptionInsufficientStockException.class,
                () -> service.dispense(prescriptionId)
        );

        assertEquals(prescriptionId, ex.getPrescriptionId());
        assertEquals(1, ex.getDetails().size());
        assertEquals(prescriptionItem.getId(), ex.getDetails().get(0).prescriptionItemId());
        assertEquals(medicineId, ex.getDetails().get(0).medicineId());
        assertEquals("MED-001", ex.getDetails().get(0).medicineCode());
        assertEquals("Paracetamol", ex.getDetails().get(0).medicineName());
        assertEquals(70, ex.getDetails().get(0).requiredQuantity());
        assertEquals(60, ex.getDetails().get(0).availableQuantity());
        assertEquals(10, ex.getDetails().get(0).shortageQuantity());
    }

    private Prescription prescription(UUID prescriptionId, UUID medicineId) {
        PrescriptionItem item = prescriptionItem(prescriptionId, medicineId, 70);
        return Prescription.restore(
                prescriptionId,
                "RX-001",
                UUID.randomUUID(),
                PrescriptionStatus.PENDING_DISPENSE,
                "note",
                UUID.randomUUID(),
                NOW.minusSeconds(600),
                null,
                null,
                List.of(item)
        );
    }

    private PrescriptionItem prescriptionItem(UUID prescriptionId, UUID medicineId, int quantity) {
        return PrescriptionItem.restore(
                UUID.randomUUID(),
                prescriptionId,
                medicineId,
                "Paracetamol",
                "Paracetamol",
                "500 mg",
                "vien",
                "1 vien",
                "2 lan/ngay",
                AdministrationRoute.ORAL,
                5,
                quantity,
                null,
                NOW.minusSeconds(600),
                null
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
                NOW.minusSeconds(3600),
                null
        );
    }

    private Medicine medicine(UUID medicineId, String code, String name) {
        return Medicine.restore(
                medicineId,
                code,
                name,
                name,
                "500 mg",
                DosageForm.TABLET,
                "vien",
                AdministrationRoute.ORAL,
                true,
                NOW.minusSeconds(86400),
                null,
                120,
                20
        );
    }
}

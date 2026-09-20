package com.benhsoan.application.ucservice.prescription;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.access.AccessDeniedException;

import com.benhsoan.domain.auditlog.AuditLog;
import com.benhsoan.domain.auditlog.enums.ActionType;
import com.benhsoan.domain.auditlog.enums.ResourceType;
import com.benhsoan.domain.billing.Payment;
import com.benhsoan.domain.billing.enums.PaymentStatus;
import com.benhsoan.domain.inventory.MedicineBatch;
import com.benhsoan.domain.inventory.enums.BatchStatus;
import com.benhsoan.domain.medicalrecord.MedicalRecord;
import com.benhsoan.domain.medicine.Medicine;
import com.benhsoan.domain.medicine.enums.AdministrationRoute;
import com.benhsoan.domain.medicine.enums.DosageForm;
import com.benhsoan.domain.prescription.Prescription;
import com.benhsoan.domain.prescription.PrescriptionDispenseItem;
import com.benhsoan.domain.prescription.PrescriptionItem;
import com.benhsoan.domain.prescription.enums.InterconnectionStatus;
import com.benhsoan.domain.prescription.enums.PrescriptionStatus;
import com.benhsoan.domain.prescription.exception.PrescriptionReturnPaymentNotRefundedException;
import com.benhsoan.domain.shared.exception.ValidationException;
import com.benhsoan.port.dto.command.prescription.ReturnMedicationCommand;
import com.benhsoan.port.dto.command.prescription.ReturnMedicationItemCommand;
import com.benhsoan.port.outbound.repository.audit.AuditLogRepository;
import com.benhsoan.port.outbound.repository.billing.PaymentRepository;
import com.benhsoan.port.outbound.repository.inventory.MedicineBatchRepository;
import com.benhsoan.port.outbound.repository.inventory.StockMovementRepository;
import com.benhsoan.port.outbound.repository.medicalrecord.MedicalRecordRepository;
import com.benhsoan.port.outbound.repository.medicine.MedicineRepository;
import com.benhsoan.port.outbound.repository.prescription.MedicationReturnRepository;
import com.benhsoan.port.outbound.repository.prescription.PrescriptionDispenseItemRepository;
import com.benhsoan.port.outbound.repository.prescription.PrescriptionRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.benhsoan.port.outbound.time.ClockPort;

class ReturnMedicationServiceTest {

    private static final Instant NOW = Instant.parse("2026-08-20T02:00:00Z");
    private static final UUID ACTOR_ID = UUID.randomUUID();
    private static final UUID MEDICINE_ID = UUID.randomUUID();
    private static final UUID BATCH_ID = UUID.randomUUID();

    private final PrescriptionRepository prescriptionRepository = mock(PrescriptionRepository.class);
    private final PrescriptionDispenseItemRepository dispenseItemRepository =
            mock(PrescriptionDispenseItemRepository.class);
    private final MedicationReturnRepository medicationReturnRepository =
            mock(MedicationReturnRepository.class);
    private final MedicineBatchRepository medicineBatchRepository = mock(MedicineBatchRepository.class);
    private final StockMovementRepository stockMovementRepository = mock(StockMovementRepository.class);
    private final MedicineRepository medicineRepository = mock(MedicineRepository.class);
    private final MedicalRecordRepository medicalRecordRepository = mock(MedicalRecordRepository.class);
    private final PaymentRepository paymentRepository = mock(PaymentRepository.class);
    private final AuditLogRepository auditLogRepository = mock(AuditLogRepository.class);
    private final CurrentUserPort currentUserPort = mock(CurrentUserPort.class);
    private final ClockPort clockPort = mock(ClockPort.class);

    private ReturnMedicationService service;

    @BeforeEach
    void setUp() {
        service = new ReturnMedicationService(
                prescriptionRepository,
                dispenseItemRepository,
                medicationReturnRepository,
                medicineBatchRepository,
                stockMovementRepository,
                medicineRepository,
                medicalRecordRepository,
                paymentRepository,
                auditLogRepository,
                currentUserPort,
                clockPort);
        when(currentUserPort.hasRole("PHARMACIST")).thenReturn(true);
        when(currentUserPort.getCurrentUserId()).thenReturn(ACTOR_ID);
        when(clockPort.now()).thenReturn(NOW);
    }

    @Test
    void returnsMedicationToOriginalBatchAndAudits() {
        UUID medicalRecordId = UUID.randomUUID();
        UUID prescriptionId = UUID.randomUUID();
        UUID itemId = UUID.randomUUID();
        UUID dispenseItemId = UUID.randomUUID();

        PrescriptionItem item = item(prescriptionId, itemId, 20, 12);
        Prescription prescription = prescription(prescriptionId, medicalRecordId,
                PrescriptionStatus.DISPENSED, item);
        PrescriptionDispenseItem dispenseItem = dispenseItem(
                dispenseItemId, prescriptionId, itemId, BATCH_ID, 12);

        when(prescriptionRepository.findByIdForUpdate(prescriptionId))
                .thenReturn(Optional.of(prescription));
        when(prescriptionRepository.save(any(Prescription.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        MedicalRecord medicalRecord = mock(MedicalRecord.class);
        when(medicalRecord.getVisitId()).thenReturn(UUID.randomUUID());
        when(medicalRecordRepository.findById(medicalRecordId)).thenReturn(Optional.of(medicalRecord));
        when(paymentRepository.findByVisitId(any())).thenReturn(Optional.empty());

        when(dispenseItemRepository.findByPrescriptionId(prescriptionId))
                .thenReturn(List.of(dispenseItem));
        when(medicineRepository.findAllById(any())).thenReturn(List.of(medicine()));
        when(medicineBatchRepository.findAllById(any())).thenReturn(List.of(batch()));

        var result = service.returnMedication(new ReturnMedicationCommand(
                prescriptionId,
                "Patient did not use",
                List.of(new ReturnMedicationItemCommand(dispenseItemId, 5))));

        assertEquals(PrescriptionStatus.PARTIALLY_DISPENSED, result.status());
        assertEquals(7, item.getDispensedQuantity());

        verify(medicineBatchRepository)
                .restoreStockQuantity(eq(BATCH_ID), eq(5), eq(BatchStatus.ACTIVE), eq(NOW));
        verify(medicationReturnRepository).save(any());
        verify(stockMovementRepository).saveAll(any());

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        assertEquals(ActionType.RETURN, captor.getValue().getActionType());
        assertEquals(ResourceType.PRESCRIPTION, captor.getValue().getResourceType());
        assertEquals(ACTOR_ID, captor.getValue().getUserId());
    }

    @Test
    void blocksReturnWhenPaymentRecordedAndNotRefunded() {
        UUID medicalRecordId = UUID.randomUUID();
        UUID prescriptionId = UUID.randomUUID();
        UUID itemId = UUID.randomUUID();
        UUID dispenseItemId = UUID.randomUUID();

        PrescriptionItem item = item(prescriptionId, itemId, 20, 12);
        Prescription prescription = prescription(prescriptionId, medicalRecordId,
                PrescriptionStatus.DISPENSED, item);

        when(prescriptionRepository.findByIdForUpdate(prescriptionId))
                .thenReturn(Optional.of(prescription));

        UUID visitId = UUID.randomUUID();
        MedicalRecord medicalRecord = mock(MedicalRecord.class);
        when(medicalRecord.getVisitId()).thenReturn(visitId);
        when(medicalRecordRepository.findById(medicalRecordId)).thenReturn(Optional.of(medicalRecord));

        Payment payment = mock(Payment.class);
        when(payment.getStatus()).thenReturn(PaymentStatus.RECORDED);
        when(paymentRepository.findByVisitId(visitId)).thenReturn(Optional.of(payment));

        assertThrows(PrescriptionReturnPaymentNotRefundedException.class,
                () -> service.returnMedication(new ReturnMedicationCommand(
                        prescriptionId,
                        "reason",
                        List.of(new ReturnMedicationItemCommand(dispenseItemId, 5)))));

        verify(medicationReturnRepository, never()).save(any());
        verify(medicineBatchRepository, never())
                .restoreStockQuantity(any(), any(int.class), any(), any());
    }

    @Test
    void rejectsReturnQuantityExceedingReturnable() {
        UUID medicalRecordId = UUID.randomUUID();
        UUID prescriptionId = UUID.randomUUID();
        UUID itemId = UUID.randomUUID();
        UUID dispenseItemId = UUID.randomUUID();

        PrescriptionItem item = item(prescriptionId, itemId, 20, 12);
        Prescription prescription = prescription(prescriptionId, medicalRecordId,
                PrescriptionStatus.DISPENSED, item);
        PrescriptionDispenseItem dispenseItem = dispenseItem(
                dispenseItemId, prescriptionId, itemId, BATCH_ID, 12);

        when(prescriptionRepository.findByIdForUpdate(prescriptionId))
                .thenReturn(Optional.of(prescription));
        MedicalRecord medicalRecord = mock(MedicalRecord.class);
        when(medicalRecord.getVisitId()).thenReturn(UUID.randomUUID());
        when(medicalRecordRepository.findById(medicalRecordId)).thenReturn(Optional.of(medicalRecord));
        when(paymentRepository.findByVisitId(any())).thenReturn(Optional.empty());
        when(dispenseItemRepository.findByPrescriptionId(prescriptionId))
                .thenReturn(List.of(dispenseItem));
        when(medicineRepository.findAllById(any())).thenReturn(List.of(medicine()));
        when(medicineBatchRepository.findAllById(any())).thenReturn(List.of(batch()));

        assertThrows(ValidationException.class,
                () -> service.returnMedication(new ReturnMedicationCommand(
                        prescriptionId,
                        "reason",
                        List.of(new ReturnMedicationItemCommand(dispenseItemId, 13)))));

        verify(medicineBatchRepository, never())
                .restoreStockQuantity(any(), any(int.class), any(), any());
    }

    @Test
    void rejectsNonPharmacist() {
        when(currentUserPort.hasRole("PHARMACIST")).thenReturn(false);
        when(currentUserPort.hasRole("ADMIN")).thenReturn(false);

        assertThrows(AccessDeniedException.class,
                () -> service.returnMedication(new ReturnMedicationCommand(
                        UUID.randomUUID(), "reason",
                        List.of(new ReturnMedicationItemCommand(UUID.randomUUID(), 1)))));
    }

    private Medicine medicine() {
        return Medicine.restore(
                MEDICINE_ID, "MED-001", "Paracetamol", "Paracetamol", "500 mg",
                DosageForm.TABLET, "vien", AdministrationRoute.ORAL, true,
                NOW.minusSeconds(86400), null, 120, 20);
    }

    private MedicineBatch batch() {
        return MedicineBatch.restore(
                BATCH_ID, MEDICINE_ID, "BATCH-A", LocalDate.of(2026, 12, 1),
                20, BatchStatus.ACTIVE, NOW.minusSeconds(3600), null);
    }

    private PrescriptionItem item(UUID prescriptionId, UUID itemId, int prescribed, int dispensed) {
        return PrescriptionItem.restore(
                itemId, prescriptionId, MEDICINE_ID, "Paracetamol", "Paracetamol",
                "500 mg", "vien", "1 vien", 2, AdministrationRoute.ORAL, 5,
                prescribed, dispensed, null, NOW.minusSeconds(600), null);
    }

    private Prescription prescription(
            UUID prescriptionId,
            UUID medicalRecordId,
            PrescriptionStatus status,
            PrescriptionItem item
    ) {
        return Prescription.restore(
                prescriptionId, "RX-001", medicalRecordId, status, "note",
                null, ACTOR_ID, NOW.minusSeconds(600), null, null,
                InterconnectionStatus.NOT_SENT, null, null, null, List.of(item));
    }

    private PrescriptionDispenseItem dispenseItem(
            UUID id, UUID prescriptionId, UUID itemId, UUID batchId, int dispensedQuantity
    ) {
        return PrescriptionDispenseItem.create(
                id, prescriptionId, itemId, MEDICINE_ID, batchId,
                dispensedQuantity, ACTOR_ID, NOW.minusSeconds(600));
    }
}


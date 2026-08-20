package com.benhsoan.application.ucservice.prescription;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.Mock;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.benhsoan.application.ucservice.prescription.snapshot.PrescriptionSnapshotMapper;
import com.benhsoan.application.ucservice.prescription.snapshot.PrescriptionSnapshotSerializer;
import com.benhsoan.domain.auditlog.AuditLog;
import com.benhsoan.domain.druginteraction.enums.InteractionSeverity;
import com.benhsoan.domain.medicine.Medicine;
import com.benhsoan.domain.medicine.enums.AdministrationRoute;
import com.benhsoan.domain.medicine.enums.DosageForm;
import com.benhsoan.domain.prescription.Prescription;
import com.benhsoan.domain.prescription.PrescriptionAmendment;
import com.benhsoan.domain.prescription.PrescriptionItem;
import com.benhsoan.domain.prescription.enums.PrescriptionStatus;
import com.benhsoan.domain.prescription.exception.PrescriptionInteractionConfirmationRequiredException;
import com.benhsoan.domain.prescription.exception.PrescriptionInvalidStatusException;
import com.benhsoan.domain.prescription.exception.PrescriptionNoChangesException;
import com.benhsoan.domain.prescription.exception.UnauthorizedPrescriptionAmendmentException;
import com.benhsoan.domain.shared.exception.ValidationException;
import com.benhsoan.port.dto.command.prescription.AmendPrescriptionCommand;
import com.benhsoan.port.dto.command.prescription.AmendPrescriptionItemCommand;
import com.benhsoan.port.dto.result.DrugInteractionWarningResult;
import com.benhsoan.port.inbound.prescription.CheckDrugInteractionUseCase;
import com.benhsoan.port.outbound.repository.audit.AuditLogRepository;
import com.benhsoan.port.outbound.repository.medicine.MedicineRepository;
import com.benhsoan.port.outbound.repository.prescription.PrescriptionAmendmentRepository;
import com.benhsoan.port.outbound.repository.prescription.PrescriptionRepository;
import com.benhsoan.port.outbound.repository.prescription.PrescriptionWarningLogRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.fasterxml.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class AmendPrescriptionServiceTest {

    private static final Instant CREATED_AT = Instant.parse("2026-08-05T02:00:00Z");
    private static final Instant AMENDED_AT = Instant.parse("2026-08-05T03:00:00Z");

    @Mock private PrescriptionRepository prescriptionRepository;
    @Mock private MedicineRepository medicineRepository;
    @Mock private CheckDrugInteractionUseCase checkDrugInteractionUseCase;
    @Mock private PrescriptionWarningLogRepository warningLogRepository;
    @Mock private PrescriptionAmendmentRepository amendmentRepository;
    @Mock private AuditLogRepository auditLogRepository;
    @Mock private CurrentUserPort currentUserPort;
    @Mock private PrescriptionClinicalContextValidator clinicalContextValidator;
    @Mock private PrescriptionDisplayContextResolver displayContextResolver;

    private AmendPrescriptionService service;
    private UUID actorId;
    private UUID prescriptionId;
    private UUID existingMedicineId;

    @BeforeEach
    void setUp() {
        lenient().when(displayContextResolver.resolve(any(), any()))
                .thenReturn(new PrescriptionDisplayContextResolver.PrescriptionDisplayContext(
                        null,
                        null,
                        null,
                        null,
                        null,
                        null
                ));
        service = new AmendPrescriptionService(
                prescriptionRepository,
                medicineRepository,
                checkDrugInteractionUseCase,
                warningLogRepository,
                amendmentRepository,
                auditLogRepository,
                currentUserPort,
                new PrescriptionResultMapper(displayContextResolver),
                new PrescriptionSnapshotMapper(),
                new PrescriptionSnapshotSerializer(new ObjectMapper().findAndRegisterModules()),
                () -> AMENDED_AT,
                clinicalContextValidator
        );
        actorId = UUID.randomUUID();
        prescriptionId = UUID.randomUUID();
        existingMedicineId = UUID.randomUUID();
        when(currentUserPort.hasRole("DOCTOR")).thenReturn(true);
        when(currentUserPort.getCurrentUserId()).thenReturn(actorId);
        lenient().when(medicineRepository.findById(existingMedicineId))
                .thenReturn(Optional.of(medicine(existingMedicineId, true)));
    }

    @Test
    void amendsPendingPrescriptionAndStoresCompleteSnapshots() {
        Prescription prescription = pendingPrescription(actorId);
        when(prescriptionRepository.findByIdForUpdate(prescriptionId)).thenReturn(Optional.of(prescription));
        when(checkDrugInteractionUseCase.check(any())).thenReturn(List.of());
        when(prescriptionRepository.save(any(Prescription.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(amendmentRepository.save(any(PrescriptionAmendment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var result = service.amend(command(List.of(item(existingMedicineId, "2 tablets"))));

        assertEquals("RX000001", result.prescriptionCode());
        assertEquals(CREATED_AT, result.prescribedAt());
        assertEquals("2 tablets", result.items().getFirst().dosage());
        assertEquals(AMENDED_AT, result.updatedAt());
        ArgumentCaptor<PrescriptionAmendment> amendmentCaptor = ArgumentCaptor.forClass(PrescriptionAmendment.class);
        verify(amendmentRepository).save(amendmentCaptor.capture());
        assertEquals("Dose adjustment", amendmentCaptor.getValue().getChangeReason());
        assertEquals(true, amendmentCaptor.getValue().getBeforeData().contains("1 tablet"));
        assertEquals(true, amendmentCaptor.getValue().getAfterData().contains("2 tablets"));
        verify(auditLogRepository).save(any(AuditLog.class));
    }

    @Test
    void rejectsAmendmentWithNoBusinessChanges() {
        Prescription prescription = pendingPrescription(actorId);
        when(prescriptionRepository.findByIdForUpdate(prescriptionId)).thenReturn(Optional.of(prescription));

        assertThrows(PrescriptionNoChangesException.class,
                () -> service.amend(command(List.of(item(existingMedicineId, "1 tablet")))));

        verify(prescriptionRepository, never()).save(any());
        verify(amendmentRepository, never()).save(any());
        verify(auditLogRepository, never()).save(any());
    }

    @Test
    void rejectsDispensedPrescriptionBeforeChangingItems() {
        Prescription prescription = prescription(actorId, PrescriptionStatus.DISPENSED);
        when(prescriptionRepository.findByIdForUpdate(prescriptionId)).thenReturn(Optional.of(prescription));

        assertThrows(PrescriptionInvalidStatusException.class,
                () -> service.amend(command(List.of(item(existingMedicineId, "2 tablets")))));

        verify(prescriptionRepository, never()).save(any());
        verify(amendmentRepository, never()).save(any());
    }

    @Test
    void rejectsDoctorWhoDidNotCreatePrescriptionWithForbiddenDomainException() {
        Prescription prescription = pendingPrescription(UUID.randomUUID());
        when(prescriptionRepository.findByIdForUpdate(prescriptionId)).thenReturn(Optional.of(prescription));

        assertThrows(UnauthorizedPrescriptionAmendmentException.class,
                () -> service.amend(command(List.of(item(existingMedicineId, "2 tablets")))));

        verify(prescriptionRepository, never()).save(any());
    }

    @Test
    void rejectsInactiveNewMedicineBeforeSavingAmendment() {
        Prescription prescription = pendingPrescription(actorId);
        UUID inactiveMedicineId = UUID.randomUUID();
        when(prescriptionRepository.findByIdForUpdate(prescriptionId)).thenReturn(Optional.of(prescription));
        when(medicineRepository.findById(inactiveMedicineId)).thenReturn(Optional.of(medicine(inactiveMedicineId, false)));

        assertThrows(ValidationException.class,
                () -> service.amend(command(List.of(item(existingMedicineId, "1 tablet"), item(inactiveMedicineId, "1 tablet")))));

        verify(prescriptionRepository, never()).save(any());
        verify(amendmentRepository, never()).save(any());
    }

    @Test
    void rejectsNewInteractionWithoutOverrideBeforeSavingAmendment() {
        Prescription prescription = pendingPrescription(actorId);
        UUID additionalMedicineId = UUID.randomUUID();
        when(prescriptionRepository.findByIdForUpdate(prescriptionId)).thenReturn(Optional.of(prescription));
        when(medicineRepository.findById(additionalMedicineId)).thenReturn(Optional.of(medicine(additionalMedicineId, true)));
        when(checkDrugInteractionUseCase.check(any()))
                .thenReturn(List.of(new DrugInteractionWarningResult(
                        UUID.randomUUID(),
                        existingMedicineId,
                        additionalMedicineId,
                        InteractionSeverity.MODERATE,
                        "Interaction detected",
                        "Monitor closely"
                )));

        assertThrows(PrescriptionInteractionConfirmationRequiredException.class,
                () -> service.amend(command(List.of(item(existingMedicineId, "1 tablet"), item(additionalMedicineId, "1 tablet")))));

        verify(prescriptionRepository, never()).save(any());
        verify(amendmentRepository, never()).save(any());
    }

    private AmendPrescriptionCommand command(List<AmendPrescriptionItemCommand> items) {
        return AmendPrescriptionCommand.builder()
                .prescriptionId(prescriptionId)
                .note("Take after meals")
                .changeReason("Dose adjustment")
                .items(items)
                .interactionOverrides(List.of())
                .build();
    }

    private AmendPrescriptionItemCommand item(UUID medicineId, String dosage) {
        return AmendPrescriptionItemCommand.builder()
                .medicineId(medicineId)
                .dosage(dosage)
                .frequency(2)
                .route(AdministrationRoute.ORAL)
                .durationDays(5)
                .quantity(10)
                .instructions(null)
                .build();
    }

    private Prescription pendingPrescription(UUID prescribedBy) {
        return prescription(prescribedBy, PrescriptionStatus.PENDING_DISPENSE);
    }

    private Prescription prescription(UUID prescribedBy, PrescriptionStatus status) {
        PrescriptionItem item = PrescriptionItem.create(UUID.randomUUID(), prescriptionId, existingMedicineId,
                "Paracetamol", "Paracetamol", "500 mg", "tablet", "1 tablet", 2,
                AdministrationRoute.ORAL, 5, 10, null, CREATED_AT);
        return Prescription.restore(prescriptionId, "RX000001", UUID.randomUUID(), status, "Take after meals",
                prescribedBy, CREATED_AT, null, null, List.of(item));
    }

    private Medicine medicine(UUID id, boolean active) {
        return Medicine.restore(id, "MED-001", "Amoxicillin", "Amoxicillin", "500 mg", DosageForm.CAPSULE,
                "capsule", AdministrationRoute.ORAL, active, CREATED_AT, null, 0, 20);
    }
}

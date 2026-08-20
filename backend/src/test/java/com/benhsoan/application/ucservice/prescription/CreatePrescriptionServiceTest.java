package com.benhsoan.application.ucservice.prescription;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import com.benhsoan.domain.auditlog.AuditLog;
import com.benhsoan.domain.auditlog.enums.ActionType;
import com.benhsoan.domain.auditlog.enums.ResourceType;
import com.benhsoan.domain.druginteraction.enums.InteractionSeverity;
import com.benhsoan.domain.medicine.Medicine;
import com.benhsoan.domain.medicine.enums.AdministrationRoute;
import com.benhsoan.domain.medicine.enums.DosageForm;
import com.benhsoan.domain.prescription.Prescription;
import com.benhsoan.domain.prescription.PrescriptionWarningLog;
import com.benhsoan.domain.prescription.enums.PrescriptionStatus;
import com.benhsoan.domain.prescription.exception.PrescriptionInteractionConfirmationRequiredException;
import com.benhsoan.domain.shared.exception.ValidationException;
import com.benhsoan.port.dto.command.prescription.CreatePrescriptionCommand;
import com.benhsoan.port.dto.command.prescription.CreatePrescriptionItemCommand;
import com.benhsoan.port.dto.command.prescription.PrescriptionInteractionOverrideCommand;
import com.benhsoan.port.dto.result.DrugInteractionWarningResult;
import com.benhsoan.port.inbound.prescription.CheckDrugInteractionUseCase;
import com.benhsoan.port.outbound.generator.PrescriptionCodeGenerator;
import com.benhsoan.port.outbound.repository.audit.AuditLogRepository;
import com.benhsoan.port.outbound.repository.medicalrecord.MedicalRecordDiagnosisRepository;
import com.benhsoan.port.outbound.repository.medicine.MedicineRepository;
import com.benhsoan.port.outbound.repository.prescription.PrescriptionRepository;
import com.benhsoan.port.outbound.repository.prescription.PrescriptionWarningLogRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;

@ExtendWith(MockitoExtension.class)
class CreatePrescriptionServiceTest {

    private static final Instant NOW = Instant.parse("2026-08-05T02:00:00Z");

    @Mock private PrescriptionRepository prescriptionRepository;
    @Mock private MedicineRepository medicineRepository;
    @Mock private CheckDrugInteractionUseCase checkDrugInteractionUseCase;
    @Mock private MedicalRecordDiagnosisRepository medicalRecordDiagnosisRepository;
    @Mock private PrescriptionWarningLogRepository warningLogRepository;
    @Mock private PrescriptionCodeGenerator prescriptionCodeGenerator;
    @Mock private CurrentUserPort currentUserPort;
    @Mock private AuditLogRepository auditLogRepository;
    @Mock private PrescriptionClinicalContextValidator clinicalContextValidator;
    @Mock private PrescriptionDisplayContextResolver displayContextResolver;

    private CreatePrescriptionService service;
    private UUID actorId;
    private UUID medicalRecordId;
    private UUID medicineId;

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
        service = new CreatePrescriptionService(
                prescriptionRepository,
                medicineRepository,
                checkDrugInteractionUseCase,
                medicalRecordDiagnosisRepository,
                warningLogRepository,
                prescriptionCodeGenerator,
                currentUserPort,
                new PrescriptionResultMapper(displayContextResolver),
                auditLogRepository,
                () -> NOW,
                clinicalContextValidator
        );
        actorId = UUID.randomUUID();
        medicalRecordId = UUID.randomUUID();
        medicineId = UUID.randomUUID();
    }

    @Test
    void createsPrescriptionWithMedicineSnapshotAndAuditLog() {
        prepareValidCreate();
        preparePersistence();
        when(checkDrugInteractionUseCase.check(any())).thenReturn(List.of());

        var result = service.create(command(List.of(item(medicineId)), List.of()));

        assertEquals("RX000001", result.prescriptionCode());
        assertEquals(PrescriptionStatus.PENDING_DISPENSE, result.status());
        assertEquals(1, result.items().size());
        assertEquals("Paracetamol", result.items().getFirst().medicineName());
        assertEquals(AdministrationRoute.TOPICAL, result.items().getFirst().route());
        assertEquals(0, result.warnings().size());

        ArgumentCaptor<Prescription> prescriptionCaptor = ArgumentCaptor.forClass(Prescription.class);
        verify(prescriptionRepository).save(prescriptionCaptor.capture());
        assertEquals(PrescriptionStatus.PENDING_DISPENSE,
                prescriptionCaptor.getValue().getStatus());
        assertEquals(actorId, prescriptionCaptor.getValue().getPrescribedBy());
        assertEquals(NOW, prescriptionCaptor.getValue().getPrescribedAt());
        ArgumentCaptor<AuditLog> auditCaptor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(auditCaptor.capture());
        assertEquals(actorId, auditCaptor.getValue().getUserId());
        assertEquals(ActionType.CREATE, auditCaptor.getValue().getActionType());
        assertEquals(ResourceType.PRESCRIPTION, auditCaptor.getValue().getResourceType());
        assertEquals(prescriptionCaptor.getValue().getId(), auditCaptor.getValue().getResourceId());
        assertEquals(NOW, auditCaptor.getValue().getCreatedAt());
        assertTrue(auditCaptor.getValue().getDetail().contains("RX000001"));
        verify(warningLogRepository, never()).save(any());
    }

    @Test
    void createsPendingDispensePrescriptionWhenTwoMedicinesAreComplete() {
        UUID secondMedicineId = UUID.randomUUID();
        prepareValidCreate();
        preparePersistence();
        when(medicineRepository.findAllById(any())).thenReturn(List.of(
                activeMedicine(medicineId),
                activeMedicine(secondMedicineId)
        ));
        when(checkDrugInteractionUseCase.check(any())).thenReturn(List.of());

        var result = service.create(command(
                List.of(item(medicineId), item(secondMedicineId)),
                List.of()
        ));

        assertEquals(PrescriptionStatus.PENDING_DISPENSE, result.status());
        assertEquals(2, result.items().size());
        verify(prescriptionRepository).save(any(Prescription.class));
    }

    @Test
    void rejectsUnconfirmedDrugInteractionBeforeSavingPrescription() {
        prepareValidCreate();
        UUID secondMedicineId = UUID.randomUUID();
        when(medicineRepository.findAllById(any())).thenReturn(
                List.of(activeMedicine(medicineId), activeMedicine(secondMedicineId)));
        when(checkDrugInteractionUseCase.check(any())).thenReturn(List.of(warning(medicineId, secondMedicineId)));

        assertThrows(PrescriptionInteractionConfirmationRequiredException.class,
                () -> service.create(command(List.of(item(medicineId), item(secondMedicineId)), List.of())));

        verify(prescriptionRepository, never()).save(any());
        verify(auditLogRepository, never()).save(any());
    }

    @Test
    void persistsWarningLogWhenInteractionIsExplicitlyOverridden() {
        prepareValidCreate();
        preparePersistence();
        UUID secondMedicineId = UUID.randomUUID();
        DrugInteractionWarningResult warning = warning(medicineId, secondMedicineId);
        when(medicineRepository.findAllById(any())).thenReturn(
                List.of(activeMedicine(medicineId), activeMedicine(secondMedicineId)));
        when(checkDrugInteractionUseCase.check(any())).thenReturn(List.of(warning));
        when(warningLogRepository.save(any(PrescriptionWarningLog.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var result = service.create(command(
                List.of(item(medicineId), item(secondMedicineId)),
                List.of(new PrescriptionInteractionOverrideCommand(
                        warning.ruleId(),
                        "Benefits outweigh the moderate interaction."
                ))
        ));

        assertEquals(1, result.warnings().size());
        assertEquals("Benefits outweigh the moderate interaction.", result.warnings().getFirst().overrideReason());
        verify(warningLogRepository).save(any(PrescriptionWarningLog.class));
        verify(prescriptionRepository).save(any(Prescription.class));
    }

    @Test
    void rejectsInactiveMedicineBeforeSavingPrescription() {
        prepareValidCreate();
        when(medicineRepository.findAllById(any())).thenReturn(List.of(inactiveMedicine(medicineId)));

        assertThrows(ValidationException.class, () -> service.create(command(List.of(item(medicineId)), List.of())));

        verify(prescriptionRepository, never()).save(any());
    }

    @Test
    void rejectsCallerWithoutDoctorRole() {
        when(currentUserPort.hasRole("DOCTOR")).thenReturn(false);

        assertThrows(AccessDeniedException.class,
                () -> service.create(command(List.of(item(medicineId)), List.of())));

        verify(prescriptionRepository, never()).save(any(Prescription.class));
    }

    @Test
    void rejectsAdministratorRoleFromCreatingPrescription() {
        when(currentUserPort.hasRole("DOCTOR")).thenReturn(false);

        assertThrows(AccessDeniedException.class,
                () -> service.create(command(List.of(item(medicineId)), List.of())));

        verify(currentUserPort, never()).hasRole("ADMIN");
        verify(prescriptionRepository, never()).save(any(Prescription.class));
    }

    private void prepareValidCreate() {
        when(currentUserPort.getCurrentUserId()).thenReturn(actorId);
        when(currentUserPort.hasRole("DOCTOR")).thenReturn(true);
        when(medicalRecordDiagnosisRepository.existsByMedicalRecordId(medicalRecordId)).thenReturn(true);
        when(medicineRepository.findAllById(any())).thenReturn(List.of(activeMedicine(medicineId)));
    }

    private void preparePersistence() {
        when(prescriptionCodeGenerator.generate()).thenReturn("RX000001");
        when(prescriptionRepository.save(any(Prescription.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    private CreatePrescriptionCommand command(
            List<CreatePrescriptionItemCommand> items,
            List<PrescriptionInteractionOverrideCommand> overrides
    ) {
        return CreatePrescriptionCommand.builder()
                .medicalRecordId(medicalRecordId)
                .note("Use after meals")
                .items(items)
                .interactionOverrides(overrides)
                .build();
    }

    private CreatePrescriptionItemCommand item(UUID id) {
        return CreatePrescriptionItemCommand.builder()
                .medicineId(id)
                .dosage("1 tablet")
                .frequency(2)
                .route(AdministrationRoute.TOPICAL)
                .durationDays(5)
                .quantity(10)
                .build();
    }

    private Medicine activeMedicine(UUID id) {
        return Medicine.restore(id, "MED001", "Paracetamol", "Paracetamol", "500 mg", DosageForm.TABLET,
                "tablet", AdministrationRoute.ORAL, true, NOW, null, 0, 20);
    }

    private Medicine inactiveMedicine(UUID id) {
        return Medicine.restore(id, "MED001", "Paracetamol", "Paracetamol", "500 mg", DosageForm.TABLET,
                "tablet", AdministrationRoute.ORAL, false, NOW, null, 0, 20);
    }

    private DrugInteractionWarningResult warning(UUID firstMedicineId, UUID secondMedicineId) {
        return new DrugInteractionWarningResult(
                UUID.randomUUID(),
                firstMedicineId,
                secondMedicineId,
                InteractionSeverity.MODERATE,
                "Interaction detected",
                "Monitor patient closely"
        );
    }
}

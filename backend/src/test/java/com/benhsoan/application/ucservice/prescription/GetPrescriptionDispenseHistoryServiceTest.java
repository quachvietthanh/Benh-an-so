package com.benhsoan.application.ucservice.prescription;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

import com.benhsoan.domain.auth.User;
import com.benhsoan.domain.inventory.MedicineBatch;
import com.benhsoan.domain.medicalrecord.MedicalRecord;
import com.benhsoan.domain.medicine.Medicine;
import com.benhsoan.domain.prescription.Prescription;
import com.benhsoan.domain.prescription.PrescriptionDispenseItem;
import com.benhsoan.domain.visit.Visit;
import com.benhsoan.port.dto.result.PrescriptionDispenseHistoryResult;
import com.benhsoan.port.outbound.repository.auth.UserRepository;
import com.benhsoan.port.outbound.repository.inventory.MedicineBatchRepository;
import com.benhsoan.port.outbound.repository.medicalrecord.MedicalRecordRepository;
import com.benhsoan.port.outbound.repository.medicine.MedicineRepository;
import com.benhsoan.port.outbound.repository.prescription.PrescriptionDispenseItemRepository;
import com.benhsoan.port.outbound.repository.prescription.PrescriptionRepository;
import com.benhsoan.port.outbound.repository.visit.VisitRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;

class GetPrescriptionDispenseHistoryServiceTest {

    private static final Instant NOW = Instant.parse("2026-08-07T02:00:00Z");
    private static final UUID DOCTOR_A = UUID.randomUUID();
    private static final UUID DOCTOR_B = UUID.randomUUID();

    private final PrescriptionRepository prescriptionRepository = mock(PrescriptionRepository.class);
    private final PrescriptionDispenseItemRepository dispenseItemRepository =
            mock(PrescriptionDispenseItemRepository.class);
    private final MedicineRepository medicineRepository = mock(MedicineRepository.class);
    private final MedicineBatchRepository medicineBatchRepository = mock(MedicineBatchRepository.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final CurrentUserPort currentUserPort = mock(CurrentUserPort.class);
    private final MedicalRecordRepository medicalRecordRepository = mock(MedicalRecordRepository.class);
    private final VisitRepository visitRepository = mock(VisitRepository.class);

    private GetPrescriptionDispenseHistoryService service;

    @BeforeEach
    void setUp() {
        PrescriptionReadAccessValidator accessValidator = new PrescriptionReadAccessValidator(
                currentUserPort,
                medicalRecordRepository,
                visitRepository
        );
        service = new GetPrescriptionDispenseHistoryService(
                prescriptionRepository,
                dispenseItemRepository,
                accessValidator,
                medicineRepository,
                medicineBatchRepository,
                userRepository
        );
    }

    @Test
    void doctorOwningVisitCanReadHistoryWithResolvedDisplayInfo() {
        UUID prescriptionId = UUID.randomUUID();
        UUID medicalRecordId = UUID.randomUUID();
        UUID visitId = UUID.randomUUID();
        UUID itemId = UUID.randomUUID();
        UUID medicineId = UUID.randomUUID();
        UUID batchId = UUID.randomUUID();
        UUID dispenserId = UUID.randomUUID();

        Prescription prescription = mock(Prescription.class);
        when(prescription.getMedicalRecordId()).thenReturn(medicalRecordId);
        when(prescriptionRepository.findById(prescriptionId)).thenReturn(Optional.of(prescription));

        stubDoctorOwningVisit(medicalRecordId, visitId, DOCTOR_A);

        PrescriptionDispenseItem event = PrescriptionDispenseItem.restore(
                UUID.randomUUID(), prescriptionId, itemId, medicineId, batchId,
                12, dispenserId, NOW, NOW);
        when(dispenseItemRepository.findByPrescriptionId(prescriptionId)).thenReturn(List.of(event));

        Medicine medicine = mock(Medicine.class);
        when(medicine.getId()).thenReturn(medicineId);
        when(medicine.getMedicineName()).thenReturn("Paracetamol");
        when(medicineRepository.findAllById(any())).thenReturn(List.of(medicine));

        MedicineBatch batch = mock(MedicineBatch.class);
        when(batch.getId()).thenReturn(batchId);
        when(batch.getBatchNumber()).thenReturn("BATCH-A");
        when(medicineBatchRepository.findAllById(any())).thenReturn(List.of(batch));

        User dispenser = mock(User.class);
        when(dispenser.getId()).thenReturn(dispenserId);
        when(dispenser.getFullName()).thenReturn("Vo Thanh Nam");
        when(userRepository.findAllById(any())).thenReturn(List.of(dispenser));

        List<PrescriptionDispenseHistoryResult> results = service.getHistory(prescriptionId);

        assertEquals(1, results.size());
        assertEquals("Paracetamol", results.get(0).medicineName());
        assertEquals("BATCH-A", results.get(0).batchNumber());
        assertEquals("Vo Thanh Nam", results.get(0).dispenserName());
    }

    @Test
    void doctorNotOwningVisitIsDenied() {
        UUID prescriptionId = UUID.randomUUID();
        UUID medicalRecordId = UUID.randomUUID();
        UUID visitId = UUID.randomUUID();

        Prescription prescription = mock(Prescription.class);
        when(prescription.getMedicalRecordId()).thenReturn(medicalRecordId);
        when(prescriptionRepository.findById(prescriptionId)).thenReturn(Optional.of(prescription));

        stubDoctorOwningVisit(medicalRecordId, visitId, DOCTOR_B);

        assertThrows(AccessDeniedException.class, () -> service.getHistory(prescriptionId));
    }

    @Test
    void managerCanReadHistoryWithoutContextualOwnershipCheck() {
        UUID prescriptionId = UUID.randomUUID();
        UUID medicalRecordId = UUID.randomUUID();

        Prescription prescription = mock(Prescription.class);
        when(prescription.getMedicalRecordId()).thenReturn(medicalRecordId);
        when(prescriptionRepository.findById(prescriptionId)).thenReturn(Optional.of(prescription));

        when(currentUserPort.hasRole("ADMIN")).thenReturn(false);
        when(currentUserPort.hasRole("PHARMACIST")).thenReturn(false);
        when(currentUserPort.hasRole("MANAGER")).thenReturn(true);
        when(dispenseItemRepository.findByPrescriptionId(prescriptionId)).thenReturn(List.of());
        when(medicineRepository.findAllById(any())).thenReturn(List.of());
        when(medicineBatchRepository.findAllById(any())).thenReturn(List.of());
        when(userRepository.findAllById(any())).thenReturn(List.of());

        assertEquals(0, service.getHistory(prescriptionId).size());
    }

    @Test
    void nonAuthorizedRoleIsDenied() {
        UUID prescriptionId = UUID.randomUUID();
        UUID medicalRecordId = UUID.randomUUID();

        Prescription prescription = mock(Prescription.class);
        when(prescription.getMedicalRecordId()).thenReturn(medicalRecordId);
        when(prescriptionRepository.findById(prescriptionId)).thenReturn(Optional.of(prescription));

        when(currentUserPort.hasRole("ADMIN")).thenReturn(false);
        when(currentUserPort.hasRole("PHARMACIST")).thenReturn(false);
        when(currentUserPort.hasRole("MANAGER")).thenReturn(false);
        when(currentUserPort.hasRole("DOCTOR")).thenReturn(false);

        assertThrows(AccessDeniedException.class, () -> service.getHistory(prescriptionId));
    }

    private void stubDoctorOwningVisit(UUID medicalRecordId, UUID visitId, UUID owningDoctorId) {
        when(currentUserPort.hasRole("ADMIN")).thenReturn(false);
        when(currentUserPort.hasRole("PHARMACIST")).thenReturn(false);
        when(currentUserPort.hasRole("MANAGER")).thenReturn(false);
        when(currentUserPort.hasRole("DOCTOR")).thenReturn(true);
        when(currentUserPort.getCurrentUserId()).thenReturn(DOCTOR_A);

        MedicalRecord record = mock(MedicalRecord.class);
        when(record.getVisitId()).thenReturn(visitId);
        when(medicalRecordRepository.findById(medicalRecordId)).thenReturn(Optional.of(record));

        Visit visit = mock(Visit.class);
        when(visit.getDoctorId()).thenReturn(owningDoctorId);
        when(visitRepository.findById(visitId)).thenReturn(Optional.of(visit));
    }
}

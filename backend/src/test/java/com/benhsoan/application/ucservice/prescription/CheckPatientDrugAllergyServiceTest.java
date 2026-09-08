package com.benhsoan.application.ucservice.prescription;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.benhsoan.domain.medicalrecord.MedicalRecord;
import com.benhsoan.domain.medicalrecord.exception.MedicalRecordNotFoundException;
import com.benhsoan.domain.medicine.Medicine;
import com.benhsoan.domain.medicine.enums.AdministrationRoute;
import com.benhsoan.domain.medicine.enums.DosageForm;
import com.benhsoan.domain.patient.PatientAllergy;
import com.benhsoan.domain.patient.enums.AllergySeverity;
import com.benhsoan.domain.shared.exception.ValidationException;
import com.benhsoan.domain.visit.Visit;
import com.benhsoan.domain.visit.enums.VisitStatus;
import com.benhsoan.domain.visit.enums.VisitType;
import com.benhsoan.port.dto.result.PatientAllergyWarningResult;
import com.benhsoan.port.outbound.repository.medicalrecord.MedicalRecordRepository;
import com.benhsoan.port.outbound.repository.medicine.MedicineRepository;
import com.benhsoan.port.outbound.repository.patient.PatientAllergyRepository;
import com.benhsoan.port.outbound.repository.visit.VisitRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("CheckPatientDrugAllergyService Unit Tests")
class CheckPatientDrugAllergyServiceTest {

    private static final Instant NOW = Instant.parse("2026-08-08T00:00:00Z");

    @Mock
    private PatientAllergyRepository patientAllergyRepository;
    @Mock
    private MedicineRepository medicineRepository;
    @Mock
    private MedicalRecordRepository medicalRecordRepository;
    @Mock
    private VisitRepository visitRepository;

    private CheckPatientDrugAllergyService service;

    private UUID medicalRecordId;
    private UUID visitId;
    private UUID patientId;
    private UUID doctorId;
    private UUID medicineId;

    @BeforeEach
    void setUp() {
        service = new CheckPatientDrugAllergyService(
                patientAllergyRepository,
                medicineRepository,
                medicalRecordRepository,
                visitRepository);
        medicalRecordId = UUID.randomUUID();
        visitId = UUID.randomUUID();
        patientId = UUID.randomUUID();
        doctorId = UUID.randomUUID();
        medicineId = UUID.randomUUID();
    }

    @Test
    void rejectsWhenMedicalRecordIdIsNull() {
        assertThrows(ValidationException.class, () -> service.check(null, List.of(medicineId)));
    }

    @Test
    void rejectsWhenMedicalRecordNotFound() {
        when(medicalRecordRepository.findById(medicalRecordId)).thenReturn(Optional.empty());
        assertThrows(MedicalRecordNotFoundException.class, () -> service.check(medicalRecordId, List.of(medicineId)));
    }

    @Test
    void rejectsWhenMedicineIdsContainsNullElement() {
        prepareContext();
        List<UUID> medicineIdsWithNull = new java.util.ArrayList<>();
        medicineIdsWithNull.add(medicineId);
        medicineIdsWithNull.add(null);

        ValidationException ex = assertThrows(ValidationException.class,
                () -> service.check(medicalRecordId, medicineIdsWithNull));
        assertEquals("Medicine ID is required.", ex.getMessage());
    }

    @Test
    void rejectsWhenVisitNotFound() {
        MedicalRecord mr = MedicalRecord.create(visitId, "Cough", "Fever", null, null, null, null, null, null, doctorId,
                NOW);
        when(medicalRecordRepository.findById(medicalRecordId)).thenReturn(Optional.of(mr));
        when(visitRepository.findById(visitId)).thenReturn(Optional.empty());

        assertThrows(ValidationException.class, () -> service.check(medicalRecordId, List.of(medicineId)));
    }

    @Test
    void returnsEmptyWhenPatientHasNoActiveAllergies() {
        prepareContext();
        when(patientAllergyRepository.findByPatientIdAndActiveTrue(patientId)).thenReturn(List.of());

        List<PatientAllergyWarningResult> results = service.check(medicalRecordId, List.of(medicineId));

        assertTrue(results.isEmpty());
    }

    @Test
    void returnsEmptyWhenNoMedicineMatchesAllergies() {
        prepareContext();
        PatientAllergy allergy = PatientAllergy.create(
                patientId, "MEDICATION", "Aspirin", AllergySeverity.MILD, "Rash", null, doctorId, NOW);
        when(patientAllergyRepository.findByPatientIdAndActiveTrue(patientId)).thenReturn(List.of(allergy));

        Medicine paracetamol = Medicine.restore(
                medicineId, "MED01", "Paracetamol", "Paracetamol", "500 mg",
                DosageForm.TABLET, "tablet", AdministrationRoute.ORAL, true, NOW, null, 0, 10);
        when(medicineRepository.findAllById(List.of(medicineId))).thenReturn(List.of(paracetamol));

        List<PatientAllergyWarningResult> results = service.check(medicalRecordId, List.of(medicineId));

        assertTrue(results.isEmpty());
    }

    @Test
    void detectsAllergyWhenIngredientMatches() {
        prepareContext();
        PatientAllergy allergy = PatientAllergy.create(
                patientId, "MEDICATION", "Amoxicillin", AllergySeverity.SEVERE, "Anaphylaxis", null, doctorId, NOW);
        when(patientAllergyRepository.findByPatientIdAndActiveTrue(patientId)).thenReturn(List.of(allergy));

        Medicine augmentin = Medicine.restore(
                medicineId, "MED02", "Augmentin 625mg", "Amoxicillin 500mg, Acid Clavulanic 125mg", "625 mg",
                DosageForm.TABLET, "tablet", AdministrationRoute.ORAL, true, NOW, null, 0, 10);
        when(medicineRepository.findAllById(List.of(medicineId))).thenReturn(List.of(augmentin));

        List<PatientAllergyWarningResult> results = service.check(medicalRecordId, List.of(medicineId));

        assertEquals(1, results.size());
        PatientAllergyWarningResult warning = results.getFirst();
        assertEquals(allergy.getId(), warning.allergyId());
        assertEquals(patientId, warning.patientId());
        assertEquals(medicineId, warning.medicineId());
        assertEquals("Augmentin 625mg", warning.medicineName());
        assertEquals("Amoxicillin 500mg, Acid Clavulanic 125mg", warning.activeIngredient());
        assertEquals("Amoxicillin", warning.allergenName());
        assertEquals(AllergySeverity.SEVERE, warning.severity());
        assertEquals("Anaphylaxis", warning.reaction());
    }

    @Test
    void deduplicatesMedicineIdsBeforeProcessing() {
        prepareContext();
        PatientAllergy allergy = PatientAllergy.create(
                patientId, "MEDICATION", "Amoxicillin", AllergySeverity.SEVERE, "Anaphylaxis", null, doctorId, NOW);
        when(patientAllergyRepository.findByPatientIdAndActiveTrue(patientId)).thenReturn(List.of(allergy));

        Medicine augmentin = Medicine.restore(
                medicineId, "MED02", "Augmentin 625mg", "Amoxicillin 500mg, Acid Clavulanic 125mg", "625 mg",
                DosageForm.TABLET, "tablet", AdministrationRoute.ORAL, true, NOW, null, 0, 10);
        when(medicineRepository.findAllById(List.of(medicineId))).thenReturn(List.of(augmentin));

        List<PatientAllergyWarningResult> results = service.check(medicalRecordId, List.of(medicineId, medicineId));

        assertEquals(1, results.size());
    }

    private void prepareContext() {
        MedicalRecord mr = MedicalRecord.create(visitId, "Cough", "Fever", null, null, null, null, null, null, doctorId,
                NOW);
        when(medicalRecordRepository.findById(medicalRecordId)).thenReturn(Optional.of(mr));
        Visit visit = Visit.restore(visitId, "VIS-001", patientId, doctorId, null, null, VisitType.APPOINTMENT,
                VisitStatus.IN_PROGRESS, NOW, NOW, null, "Checkup", null, doctorId, NOW, NOW);
        when(visitRepository.findById(visitId)).thenReturn(Optional.of(visit));
    }
}

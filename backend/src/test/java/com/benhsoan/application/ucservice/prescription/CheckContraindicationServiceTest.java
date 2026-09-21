package com.benhsoan.application.ucservice.prescription;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.benhsoan.domain.contraindication.ContraindicationRule;
import com.benhsoan.domain.contraindication.enums.ContraindicationSeverity;
import com.benhsoan.domain.contraindication.enums.ContraindicationType;
import com.benhsoan.domain.medicine.Medicine;
import com.benhsoan.domain.patient.Patient;
import com.benhsoan.domain.patient.PatientChronicDisease;
import com.benhsoan.domain.patient.enums.Gender;
import com.benhsoan.domain.patient.enums.PregnancyStatus;
import com.benhsoan.port.dto.result.ContraindicationCheckResult;
import com.benhsoan.port.outbound.repository.contraindication.ContraindicationRuleRepository;
import com.benhsoan.port.outbound.repository.medicine.MedicineRepository;
import com.benhsoan.port.outbound.repository.patient.PatientChronicDiseaseRepository;
import com.benhsoan.port.outbound.repository.patient.PatientRepository;
import com.benhsoan.port.outbound.repository.visit.VisitRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.benhsoan.port.outbound.time.ClockPort;

@ExtendWith(MockitoExtension.class)
class CheckContraindicationServiceTest {

    private static final Instant NOW = Instant.parse("2026-08-20T00:00:00Z");

    @Mock private PatientRepository patientRepository;
    @Mock private PatientChronicDiseaseRepository patientChronicDiseaseRepository;
    @Mock private ContraindicationRuleRepository ruleRepository;
    @Mock private MedicineRepository medicineRepository;
    @Mock private VisitRepository visitRepository;
    @Mock private CurrentUserPort currentUserPort;
    @Mock private ClockPort clockPort;

    private CheckContraindicationService service;
    private UUID patientId;
    private UUID medicineId;

    @BeforeEach
    void setUp() {
        service = new CheckContraindicationService(
                patientRepository,
                patientChronicDiseaseRepository,
                ruleRepository,
                medicineRepository,
                visitRepository,
                mock(PrescriptionClinicalContextValidator.class),
                currentUserPort,
                clockPort
        );
        patientId = UUID.randomUUID();
        medicineId = UUID.randomUUID();
        when(clockPort.now()).thenReturn(NOW);
    }

    @Test
    void warnsForAgeContraindication() {
        Patient patient = patient(LocalDate.of(2010, 1, 1), PregnancyStatus.NOT_PREGNANT);
        Medicine medicine = medicine("Aspirin");
        when(patientRepository.findById(patientId)).thenReturn(Optional.of(patient));
        when(patientChronicDiseaseRepository.findByPatientIdAndActiveTrue(patientId)).thenReturn(List.of());
        when(medicineRepository.findAllById(List.of(medicineId))).thenReturn(List.of(medicine));
        when(ruleRepository.findActiveByMedicineIdsAndIngredients(
                List.of(medicineId), List.of("Aspirin"))).thenReturn(List.of(ageRule()));

        ContraindicationCheckResult result = service.checkByPatientId(patientId, List.of(medicineId));

        assertEquals(1, result.warnings().size());
        assertEquals(ContraindicationType.AGE, result.warnings().getFirst().type());
        assertTrue(result.missingData().isEmpty());
    }

    @Test
    void warnsForPregnancyContraindication() {
        Patient patient = patient(LocalDate.of(1990, 1, 1), PregnancyStatus.PREGNANT);
        Medicine medicine = medicine("Ibuprofen");
        when(patientRepository.findById(patientId)).thenReturn(Optional.of(patient));
        when(patientChronicDiseaseRepository.findByPatientIdAndActiveTrue(patientId)).thenReturn(List.of());
        when(medicineRepository.findAllById(List.of(medicineId))).thenReturn(List.of(medicine));
        when(ruleRepository.findActiveByMedicineIdsAndIngredients(
                List.of(medicineId), List.of("Ibuprofen"))).thenReturn(List.of(pregnancyRule()));

        ContraindicationCheckResult result = service.checkByPatientId(patientId, List.of(medicineId));

        assertEquals(1, result.warnings().size());
        assertEquals(ContraindicationType.PREGNANCY, result.warnings().getFirst().type());
    }

    @Test
    void warnsForDiseaseContraindication() {
        UUID diagnosisCatalogId = UUID.randomUUID();
        Patient patient = patient(LocalDate.of(1960, 1, 1), PregnancyStatus.NOT_PREGNANT);
        Medicine medicine = medicine("Ibuprofen");
        when(patientRepository.findById(patientId)).thenReturn(Optional.of(patient));
        when(patientChronicDiseaseRepository.findByPatientIdAndActiveTrue(patientId))
                .thenReturn(List.of(PatientChronicDisease.create(patientId, diagnosisCatalogId, 2010, null, patientId, NOW)));
        when(medicineRepository.findAllById(List.of(medicineId))).thenReturn(List.of(medicine));
        when(ruleRepository.findActiveByMedicineIdsAndIngredients(
                List.of(medicineId), List.of("Ibuprofen"))).thenReturn(List.of(diseaseRule(diagnosisCatalogId)));

        ContraindicationCheckResult result = service.checkByPatientId(patientId, List.of(medicineId));

        assertEquals(1, result.warnings().size());
        assertEquals(ContraindicationType.DISEASE, result.warnings().getFirst().type());
    }

    @Test
    void reportsMissingDateOfBirthForAgeRule() {
        Patient patient = patient(null, PregnancyStatus.NOT_PREGNANT);
        Medicine medicine = medicine("Aspirin");
        when(patientRepository.findById(patientId)).thenReturn(Optional.of(patient));
        when(patientChronicDiseaseRepository.findByPatientIdAndActiveTrue(patientId)).thenReturn(List.of());
        when(medicineRepository.findAllById(List.of(medicineId))).thenReturn(List.of(medicine));
        when(ruleRepository.findActiveByMedicineIdsAndIngredients(
                List.of(medicineId), List.of("Aspirin"))).thenReturn(List.of(ageRule()));

        ContraindicationCheckResult result = service.checkByPatientId(patientId, List.of(medicineId));

        assertTrue(result.warnings().isEmpty());
        assertEquals(1, result.missingData().size());
        assertEquals(ContraindicationType.AGE, result.missingData().getFirst().type());
    }

    @Test
    void reportsMissingPregnancyStatusForPregnancyRule() {
        Patient patient = patient(LocalDate.of(1990, 1, 1), null);
        Medicine medicine = medicine("Ibuprofen");
        when(patientRepository.findById(patientId)).thenReturn(Optional.of(patient));
        when(patientChronicDiseaseRepository.findByPatientIdAndActiveTrue(patientId)).thenReturn(List.of());
        when(medicineRepository.findAllById(List.of(medicineId))).thenReturn(List.of(medicine));
        when(ruleRepository.findActiveByMedicineIdsAndIngredients(
                List.of(medicineId), List.of("Ibuprofen"))).thenReturn(List.of(pregnancyRule()));

        ContraindicationCheckResult result = service.checkByPatientId(patientId, List.of(medicineId));

        assertTrue(result.warnings().isEmpty());
        assertEquals(1, result.missingData().size());
        assertEquals(ContraindicationType.PREGNANCY, result.missingData().getFirst().type());
    }

    @Test
    void doesNotReportMissingDataWhenNoChronicDiseaseRecorded() {
        Patient patient = patient(LocalDate.of(1960, 1, 1), PregnancyStatus.NOT_PREGNANT);
        Medicine medicine = medicine("Ibuprofen");
        when(patientRepository.findById(patientId)).thenReturn(Optional.of(patient));
        when(patientChronicDiseaseRepository.findByPatientIdAndActiveTrue(patientId)).thenReturn(List.of());
        when(medicineRepository.findAllById(List.of(medicineId))).thenReturn(List.of(medicine));
        when(ruleRepository.findActiveByMedicineIdsAndIngredients(
                List.of(medicineId), List.of("Ibuprofen"))).thenReturn(List.of(diseaseRule(UUID.randomUUID())));

        ContraindicationCheckResult result = service.checkByPatientId(patientId, List.of(medicineId));

        assertTrue(result.warnings().isEmpty());
        assertTrue(result.missingData().isEmpty());
    }

    @Test
    void doesNotWarnForUnrelatedChronicDisease() {
        Patient patient = patient(LocalDate.of(1960, 1, 1), PregnancyStatus.NOT_PREGNANT);
        Medicine medicine = medicine("Ibuprofen");
        when(patientRepository.findById(patientId)).thenReturn(Optional.of(patient));
        when(patientChronicDiseaseRepository.findByPatientIdAndActiveTrue(patientId))
                .thenReturn(List.of(PatientChronicDisease.create(patientId, UUID.randomUUID(), 2010, null, patientId, NOW)));
        when(medicineRepository.findAllById(List.of(medicineId))).thenReturn(List.of(medicine));
        when(ruleRepository.findActiveByMedicineIdsAndIngredients(
                List.of(medicineId), List.of("Ibuprofen"))).thenReturn(List.of(diseaseRule(UUID.randomUUID())));

        ContraindicationCheckResult result = service.checkByPatientId(patientId, List.of(medicineId));

        assertTrue(result.warnings().isEmpty());
        assertTrue(result.missingData().isEmpty());
    }

    @Test
    void warnsWhenOneOfMultipleDiseasesMatchesRule() {
        UUID matchingDiagnosis = UUID.randomUUID();
        Patient patient = patient(LocalDate.of(1960, 1, 1), PregnancyStatus.NOT_PREGNANT);
        Medicine medicine = medicine("Ibuprofen");
        when(patientRepository.findById(patientId)).thenReturn(Optional.of(patient));
        when(patientChronicDiseaseRepository.findByPatientIdAndActiveTrue(patientId))
                .thenReturn(List.of(
                        PatientChronicDisease.create(patientId, UUID.randomUUID(), 2005, null, patientId, NOW),
                        PatientChronicDisease.create(patientId, matchingDiagnosis, 2010, null, patientId, NOW)));
        when(medicineRepository.findAllById(List.of(medicineId))).thenReturn(List.of(medicine));
        when(ruleRepository.findActiveByMedicineIdsAndIngredients(
                List.of(medicineId), List.of("Ibuprofen"))).thenReturn(List.of(diseaseRule(matchingDiagnosis)));

        ContraindicationCheckResult result = service.checkByPatientId(patientId, List.of(medicineId));

        assertEquals(1, result.warnings().size());
        assertEquals(ContraindicationType.DISEASE, result.warnings().getFirst().type());
    }

    @Test
    void malePatientDoesNotTriggerPregnancyMissingDataOrWarning() {
        Patient patient = patient(LocalDate.of(1990, 1, 1), null, Gender.MALE);
        Medicine medicine = medicine("Ibuprofen");
        when(patientRepository.findById(patientId)).thenReturn(Optional.of(patient));
        when(patientChronicDiseaseRepository.findByPatientIdAndActiveTrue(patientId)).thenReturn(List.of());
        when(medicineRepository.findAllById(List.of(medicineId))).thenReturn(List.of(medicine));
        when(ruleRepository.findActiveByMedicineIdsAndIngredients(
                List.of(medicineId), List.of("Ibuprofen"))).thenReturn(List.of(pregnancyRule()));

        ContraindicationCheckResult result = service.checkByPatientId(patientId, List.of(medicineId));

        assertTrue(result.warnings().isEmpty());
        assertTrue(result.missingData().isEmpty());
    }

    @Test
    void nonPregnantFemaleDoesNotTriggerPregnancyWarning() {
        Patient patient = patient(LocalDate.of(1990, 1, 1), PregnancyStatus.NOT_PREGNANT, Gender.FEMALE);
        Medicine medicine = medicine("Ibuprofen");
        when(patientRepository.findById(patientId)).thenReturn(Optional.of(patient));
        when(patientChronicDiseaseRepository.findByPatientIdAndActiveTrue(patientId)).thenReturn(List.of());
        when(medicineRepository.findAllById(List.of(medicineId))).thenReturn(List.of(medicine));
        when(ruleRepository.findActiveByMedicineIdsAndIngredients(
                List.of(medicineId), List.of("Ibuprofen"))).thenReturn(List.of(pregnancyRule()));

        ContraindicationCheckResult result = service.checkByPatientId(patientId, List.of(medicineId));

        assertTrue(result.warnings().isEmpty());
        assertTrue(result.missingData().isEmpty());
    }

    @Test
    void returnsNoWarningsWhenPatientIsSafe() {
        Patient patient = patient(LocalDate.of(1985, 1, 1), PregnancyStatus.NOT_PREGNANT);
        Medicine medicine = medicine("Paracetamol");
        when(patientRepository.findById(patientId)).thenReturn(Optional.of(patient));
        when(patientChronicDiseaseRepository.findByPatientIdAndActiveTrue(patientId))
                .thenReturn(List.of(PatientChronicDisease.create(patientId, UUID.randomUUID(), 2010, null, patientId, NOW)));
        when(medicineRepository.findAllById(List.of(medicineId))).thenReturn(List.of(medicine));
        when(ruleRepository.findActiveByMedicineIdsAndIngredients(
                List.of(medicineId), List.of("Paracetamol"))).thenReturn(List.of());

        ContraindicationCheckResult result = service.checkByPatientId(patientId, List.of(medicineId));

        assertTrue(result.warnings().isEmpty());
        assertTrue(result.missingData().isEmpty());
    }

    private Patient patient(LocalDate dateOfBirth, PregnancyStatus pregnancyStatus) {
        return patient(dateOfBirth, pregnancyStatus, Gender.FEMALE);
    }

    private Patient patient(LocalDate dateOfBirth, PregnancyStatus pregnancyStatus, Gender gender) {
        Patient patient = mock(Patient.class);
        lenient().when(patient.getId()).thenReturn(patientId);
        lenient().when(patient.getDateOfBirth()).thenReturn(dateOfBirth);
        lenient().when(patient.getPregnancyStatus()).thenReturn(pregnancyStatus);
        lenient().when(patient.getGender()).thenReturn(gender);
        return patient;
    }

    private Medicine medicine(String activeIngredient) {
        Medicine medicine = mock(Medicine.class);
        lenient().when(medicine.getId()).thenReturn(medicineId);
        lenient().when(medicine.getMedicineName()).thenReturn(activeIngredient + " med");
        lenient().when(medicine.getActiveIngredient()).thenReturn(activeIngredient);
        return medicine;
    }

    private ContraindicationRule ageRule() {
        return ContraindicationRule.restore(
                UUID.randomUUID(), null, "Aspirin", ContraindicationType.AGE,
                null, 15, null, ContraindicationSeverity.CONTRAINDICATED,
                "Contraindicated under 16", "Use Paracetamol", true, NOW, null);
    }

    private ContraindicationRule pregnancyRule() {
        return ContraindicationRule.restore(
                UUID.randomUUID(), null, "Ibuprofen", ContraindicationType.PREGNANCY,
                null, null, null, ContraindicationSeverity.CONTRAINDICATED,
                "Contraindicated in pregnancy", "Use Paracetamol", true, NOW, null);
    }

    private ContraindicationRule diseaseRule(UUID diagnosisCatalogId) {
        return ContraindicationRule.restore(
                UUID.randomUUID(), null, "Ibuprofen", ContraindicationType.DISEASE,
                null, null, diagnosisCatalogId, ContraindicationSeverity.MODERATE,
                "NSAIDs may raise blood pressure", "Use Paracetamol", true, NOW, null);
    }
}


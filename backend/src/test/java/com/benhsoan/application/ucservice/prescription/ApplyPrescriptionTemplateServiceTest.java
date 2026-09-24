package com.benhsoan.application.ucservice.prescription;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import com.benhsoan.domain.medicalrecord.DiagnosisCatalog;
import com.benhsoan.domain.medicine.Medicine;
import com.benhsoan.domain.medicine.enums.AdministrationRoute;
import com.benhsoan.domain.contraindication.enums.ContraindicationSeverity;
import com.benhsoan.domain.contraindication.enums.ContraindicationType;
import com.benhsoan.domain.druginteraction.enums.InteractionSeverity;
import com.benhsoan.domain.patient.enums.AllergySeverity;
import com.benhsoan.domain.prescription.PrescriptionTemplate;
import com.benhsoan.domain.prescription.PrescriptionTemplateItem;
import com.benhsoan.domain.prescription.exception.PrescriptionTemplateNotFoundException;
import com.benhsoan.port.dto.command.prescription.ApplyPrescriptionTemplateCommand;
import com.benhsoan.port.dto.result.AppliedPrescriptionTemplateResult;
import com.benhsoan.port.dto.result.ContraindicationCheckResult;
import com.benhsoan.port.dto.result.ContraindicationMissingDataResult;
import com.benhsoan.port.dto.result.ContraindicationWarningResult;
import com.benhsoan.port.dto.result.DrugInteractionWarningResult;
import com.benhsoan.port.dto.result.PatientAllergyWarningResult;
import com.benhsoan.port.inbound.prescription.CheckContraindicationUseCase;
import com.benhsoan.port.inbound.prescription.CheckDrugInteractionUseCase;
import com.benhsoan.port.inbound.prescription.CheckPatientDrugAllergyUseCase;
import com.benhsoan.port.outbound.repository.medicalrecord.DiagnosisCatalogRepository;
import com.benhsoan.port.outbound.repository.medicine.MedicineRepository;
import com.benhsoan.port.outbound.repository.prescription.PrescriptionTemplateRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;

@ExtendWith(MockitoExtension.class)
class ApplyPrescriptionTemplateServiceTest {

    private static final Instant NOW = Instant.parse("2026-09-30T01:00:00Z");
    private static final UUID DOCTOR_ID = UUID.randomUUID();
    private static final UUID TEMPLATE_ID = UUID.randomUUID();
    private static final UUID DIAGNOSIS_ID = UUID.randomUUID();
    private static final UUID MEDICAL_RECORD_ID = UUID.randomUUID();

    @Mock private PrescriptionTemplateRepository templateRepository;
    @Mock private DiagnosisCatalogRepository diagnosisCatalogRepository;
    @Mock private MedicineRepository medicineRepository;
    @Mock private CheckDrugInteractionUseCase checkDrugInteractionUseCase;
    @Mock private CheckPatientDrugAllergyUseCase checkPatientDrugAllergyUseCase;
    @Mock private CheckContraindicationUseCase checkContraindicationUseCase;
    @Mock private CurrentUserPort currentUserPort;

    private ApplyPrescriptionTemplateService service;

    @BeforeEach
    void setUp() {
        service = new ApplyPrescriptionTemplateService(
                templateRepository, diagnosisCatalogRepository, medicineRepository,
                checkDrugInteractionUseCase, checkPatientDrugAllergyUseCase,
                checkContraindicationUseCase, currentUserPort);
    }

    @Test
    void appliesTemplateAndReturnsDraftWithoutPersisting() {
        when(currentUserPort.hasRole("DOCTOR")).thenReturn(true);
        when(currentUserPort.getCurrentUserId()).thenReturn(DOCTOR_ID);

        Medicine medicine = activeMedicine();
        UUID medicineId = medicine.getId();
        when(templateRepository.findById(TEMPLATE_ID))
                .thenReturn(Optional.of(template(List.of(medicineId))));

        DiagnosisCatalog diagnosis = mock(DiagnosisCatalog.class);
        when(diagnosis.getCode()).thenReturn("J06.9");
        when(diagnosis.getName()).thenReturn("Viêm họng");
        when(diagnosisCatalogRepository.findById(DIAGNOSIS_ID)).thenReturn(Optional.of(diagnosis));

        when(medicineRepository.findById(medicineId)).thenReturn(Optional.of(medicine));
        when(checkDrugInteractionUseCase.check(any())).thenReturn(List.of());
        when(checkPatientDrugAllergyUseCase.check(eq(MEDICAL_RECORD_ID), anyList())).thenReturn(List.of());
        when(checkContraindicationUseCase.check(eq(MEDICAL_RECORD_ID), anyList()))
                .thenReturn(new ContraindicationCheckResult(List.of(), List.of()));

        AppliedPrescriptionTemplateResult result = service.apply(
                new ApplyPrescriptionTemplateCommand(TEMPLATE_ID, MEDICAL_RECORD_ID));

        assertEquals(1, result.items().size());
        assertTrue(result.skippedItems().isEmpty());
        assertEquals("J06.9", result.diagnosisCode());
        verify(checkDrugInteractionUseCase).check(any());
        // Patient context must be forwarded to the safety services that enforce
        // doctor/visit ownership (STEP 6): the actual medicalRecordId, not a mock.
        verify(checkPatientDrugAllergyUseCase).check(eq(MEDICAL_RECORD_ID), anyList());
        verify(checkContraindicationUseCase).check(eq(MEDICAL_RECORD_ID), anyList());
    }

    @Test
    void missingMedicineReferenceIsSkippedWithMissingReason() {
        when(currentUserPort.hasRole("DOCTOR")).thenReturn(true);
        when(currentUserPort.getCurrentUserId()).thenReturn(DOCTOR_ID);

        UUID missingMedicineId = UUID.randomUUID();
        when(templateRepository.findById(TEMPLATE_ID))
                .thenReturn(Optional.of(template(List.of(missingMedicineId))));
        when(diagnosisCatalogRepository.findById(DIAGNOSIS_ID))
                .thenReturn(Optional.of(mock(DiagnosisCatalog.class)));
        when(medicineRepository.findById(missingMedicineId)).thenReturn(Optional.empty());
        when(checkDrugInteractionUseCase.check(any())).thenReturn(List.of());
        when(checkPatientDrugAllergyUseCase.check(eq(MEDICAL_RECORD_ID), anyList())).thenReturn(List.of());
        when(checkContraindicationUseCase.check(eq(MEDICAL_RECORD_ID), anyList()))
                .thenReturn(new ContraindicationCheckResult(List.of(), List.of()));

        AppliedPrescriptionTemplateResult result = service.apply(
                new ApplyPrescriptionTemplateCommand(TEMPLATE_ID, MEDICAL_RECORD_ID));

        assertTrue(result.items().isEmpty());
        assertEquals(1, result.skippedItems().size());
        // Missing reference is reported distinctly from a discontinued medicine (TC-03).
        assertEquals(missingMedicineId, result.skippedItems().get(0).medicineId());
        assertEquals("Không tìm thấy thuốc trong danh mục", result.skippedItems().get(0).reason());
    }

    @Test
    void discontinuedMedicineIsSkippedAndReported() {
        when(currentUserPort.hasRole("DOCTOR")).thenReturn(true);
        when(currentUserPort.getCurrentUserId()).thenReturn(DOCTOR_ID);

        Medicine discontinued = mock(Medicine.class);
        UUID discontinuedId = UUID.randomUUID();
        when(discontinued.isActive()).thenReturn(false);
        when(discontinued.getMedicineName()).thenReturn("Thuốc cũ");

        when(templateRepository.findById(TEMPLATE_ID))
                .thenReturn(Optional.of(template(List.of(discontinuedId))));
        when(diagnosisCatalogRepository.findById(DIAGNOSIS_ID))
                .thenReturn(Optional.of(mock(DiagnosisCatalog.class)));
        when(medicineRepository.findById(discontinuedId)).thenReturn(Optional.of(discontinued));
        when(checkDrugInteractionUseCase.check(any())).thenReturn(List.of());
        when(checkPatientDrugAllergyUseCase.check(any(), anyList())).thenReturn(List.of());
        when(checkContraindicationUseCase.check(any(), anyList()))
                .thenReturn(new ContraindicationCheckResult(List.of(), List.of()));

        AppliedPrescriptionTemplateResult result = service.apply(
                new ApplyPrescriptionTemplateCommand(TEMPLATE_ID, MEDICAL_RECORD_ID));

        assertTrue(result.items().isEmpty());
        assertEquals(1, result.skippedItems().size());
        assertEquals("Thuốc cũ", result.skippedItems().get(0).medicineName());
    }

    @Test
    void foreignDoctorTemplateIsRejected() {
        when(currentUserPort.hasRole("DOCTOR")).thenReturn(true);
        when(currentUserPort.getCurrentUserId()).thenReturn(DOCTOR_ID);
        when(templateRepository.findById(TEMPLATE_ID)).thenReturn(Optional.of(
                PrescriptionTemplate.restore(TEMPLATE_ID, DIAGNOSIS_ID, UUID.randomUUID(), NOW,
                        List.of(item(UUID.randomUUID())))));

        assertThrows(AccessDeniedException.class,
                () -> service.apply(new ApplyPrescriptionTemplateCommand(TEMPLATE_ID, MEDICAL_RECORD_ID)));
    }

    @Test
    void missingTemplateIsRejected() {
        when(currentUserPort.hasRole("DOCTOR")).thenReturn(true);
        when(currentUserPort.getCurrentUserId()).thenReturn(DOCTOR_ID);
        when(templateRepository.findById(TEMPLATE_ID)).thenReturn(Optional.empty());

        assertThrows(PrescriptionTemplateNotFoundException.class,
                () -> service.apply(new ApplyPrescriptionTemplateCommand(TEMPLATE_ID, MEDICAL_RECORD_ID)));
    }

    @Test
    void interactionWarningIsIncludedInResult() {
        when(currentUserPort.hasRole("DOCTOR")).thenReturn(true);
        when(currentUserPort.getCurrentUserId()).thenReturn(DOCTOR_ID);

        Medicine medicine = activeMedicine();
        UUID medicineId = medicine.getId();
        when(templateRepository.findById(TEMPLATE_ID))
                .thenReturn(Optional.of(template(List.of(medicineId))));
        when(diagnosisCatalogRepository.findById(DIAGNOSIS_ID))
                .thenReturn(Optional.of(mock(DiagnosisCatalog.class)));
        when(medicineRepository.findById(medicineId)).thenReturn(Optional.of(medicine));

        UUID ruleId = UUID.randomUUID();
        UUID otherMedicineId = UUID.randomUUID();
        when(checkDrugInteractionUseCase.check(any())).thenReturn(List.of(
                new DrugInteractionWarningResult(
                        ruleId, medicineId, otherMedicineId,
                        InteractionSeverity.MODERATE, "Tương tác", "Theo dõi sát")));
        when(checkPatientDrugAllergyUseCase.check(eq(MEDICAL_RECORD_ID), anyList())).thenReturn(List.of());
        when(checkContraindicationUseCase.check(eq(MEDICAL_RECORD_ID), anyList()))
                .thenReturn(new ContraindicationCheckResult(List.of(), List.of()));

        AppliedPrescriptionTemplateResult result = service.apply(
                new ApplyPrescriptionTemplateCommand(TEMPLATE_ID, MEDICAL_RECORD_ID));

        assertEquals(1, result.interactionWarnings().size());
        assertEquals(ruleId, result.interactionWarnings().get(0).ruleId());
    }

    @Test
    void allergyWarningIsIncludedInResult() {
        when(currentUserPort.hasRole("DOCTOR")).thenReturn(true);
        when(currentUserPort.getCurrentUserId()).thenReturn(DOCTOR_ID);

        Medicine medicine = activeMedicine();
        UUID medicineId = medicine.getId();
        when(templateRepository.findById(TEMPLATE_ID))
                .thenReturn(Optional.of(template(List.of(medicineId))));
        when(diagnosisCatalogRepository.findById(DIAGNOSIS_ID))
                .thenReturn(Optional.of(mock(DiagnosisCatalog.class)));
        when(medicineRepository.findById(medicineId)).thenReturn(Optional.of(medicine));
        when(checkDrugInteractionUseCase.check(any())).thenReturn(List.of());

        UUID allergyId = UUID.randomUUID();
        when(checkPatientDrugAllergyUseCase.check(eq(MEDICAL_RECORD_ID), anyList())).thenReturn(List.of(
                new PatientAllergyWarningResult(
                        allergyId, UUID.randomUUID(), medicineId, "Paracetamol",
                        "Paracetamol", "Paracetamol", AllergySeverity.SEVERE, "Nổi mề đay")));
        when(checkContraindicationUseCase.check(eq(MEDICAL_RECORD_ID), anyList()))
                .thenReturn(new ContraindicationCheckResult(List.of(), List.of()));

        AppliedPrescriptionTemplateResult result = service.apply(
                new ApplyPrescriptionTemplateCommand(TEMPLATE_ID, MEDICAL_RECORD_ID));

        assertEquals(1, result.allergyWarnings().size());
        assertEquals(allergyId, result.allergyWarnings().get(0).allergyId());
    }

    @Test
    void contraindicationWarningAndMissingDataAreIncludedInResult() {
        when(currentUserPort.hasRole("DOCTOR")).thenReturn(true);
        when(currentUserPort.getCurrentUserId()).thenReturn(DOCTOR_ID);

        Medicine medicine = activeMedicine();
        UUID medicineId = medicine.getId();
        when(templateRepository.findById(TEMPLATE_ID))
                .thenReturn(Optional.of(template(List.of(medicineId))));
        when(diagnosisCatalogRepository.findById(DIAGNOSIS_ID))
                .thenReturn(Optional.of(mock(DiagnosisCatalog.class)));
        when(medicineRepository.findById(medicineId)).thenReturn(Optional.of(medicine));
        when(checkDrugInteractionUseCase.check(any())).thenReturn(List.of());
        when(checkPatientDrugAllergyUseCase.check(eq(MEDICAL_RECORD_ID), anyList())).thenReturn(List.of());

        UUID ruleId = UUID.randomUUID();
        when(checkContraindicationUseCase.check(eq(MEDICAL_RECORD_ID), anyList()))
                .thenReturn(new ContraindicationCheckResult(
                        List.of(new ContraindicationWarningResult(
                                UUID.randomUUID(), ruleId, medicineId, "Paracetamol",
                                ContraindicationType.AGE, ContraindicationSeverity.MODERATE,
                                "Cảnh báo tuổi", "Khuyến cáo")),
                        List.of(new ContraindicationMissingDataResult(
                                medicineId, "Paracetamol", ContraindicationType.PREGNANCY, "Thiếu dữ liệu"))));

        AppliedPrescriptionTemplateResult result = service.apply(
                new ApplyPrescriptionTemplateCommand(TEMPLATE_ID, MEDICAL_RECORD_ID));

        assertEquals(1, result.contraindicationWarnings().size());
        assertEquals(ruleId, result.contraindicationWarnings().get(0).ruleId());
        assertEquals(1, result.contraindicationMissingData().size());
    }

    private Medicine activeMedicine() {
        Medicine medicine = mock(Medicine.class);
        when(medicine.getId()).thenReturn(UUID.randomUUID());
        when(medicine.isActive()).thenReturn(true);
        when(medicine.getMedicineCode()).thenReturn("MED001");
        when(medicine.getMedicineName()).thenReturn("Paracetamol");
        when(medicine.getActiveIngredient()).thenReturn("Paracetamol");
        when(medicine.getStrength()).thenReturn("500mg");
        when(medicine.getUnit()).thenReturn("viên");
        return medicine;
    }

    private PrescriptionTemplate template(List<UUID> medicineIds) {
        return PrescriptionTemplate.restore(
                TEMPLATE_ID, DIAGNOSIS_ID, DOCTOR_ID, NOW,
                medicineIds.stream().map(this::item).toList());
    }

    private PrescriptionTemplateItem item(UUID medicineId) {
        return PrescriptionTemplateItem.create(
                TEMPLATE_ID, medicineId, "1 viên", 2, AdministrationRoute.ORAL, 7, 14, null, 0);
    }
}


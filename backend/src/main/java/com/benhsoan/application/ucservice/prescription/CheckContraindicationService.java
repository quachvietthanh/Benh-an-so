package com.benhsoan.application.ucservice.prescription;

import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.domain.contraindication.ContraindicationRule;
import com.benhsoan.domain.contraindication.enums.ContraindicationType;
import com.benhsoan.domain.medicalrecord.MedicalRecord;
import com.benhsoan.domain.medicine.Medicine;
import com.benhsoan.domain.patient.Patient;
import com.benhsoan.domain.patient.PatientChronicDisease;
import com.benhsoan.domain.patient.PatientMinorPolicy;
import com.benhsoan.domain.patient.enums.Gender;
import com.benhsoan.domain.patient.enums.PregnancyStatus;
import com.benhsoan.domain.shared.exception.ValidationException;
import com.benhsoan.domain.visit.Visit;
import com.benhsoan.port.dto.result.ContraindicationCheckResult;
import com.benhsoan.port.dto.result.ContraindicationMissingDataResult;
import com.benhsoan.port.dto.result.ContraindicationWarningResult;
import com.benhsoan.port.inbound.prescription.CheckContraindicationUseCase;
import com.benhsoan.port.outbound.repository.contraindication.ContraindicationRuleRepository;
import com.benhsoan.port.outbound.repository.medicine.MedicineRepository;
import com.benhsoan.port.outbound.repository.patient.PatientChronicDiseaseRepository;
import com.benhsoan.port.outbound.repository.patient.PatientRepository;
import com.benhsoan.port.outbound.repository.visit.VisitRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.benhsoan.port.outbound.time.ClockPort;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CheckContraindicationService implements CheckContraindicationUseCase {

    private final PatientRepository patientRepository;
    private final PatientChronicDiseaseRepository patientChronicDiseaseRepository;
    private final ContraindicationRuleRepository ruleRepository;
    private final MedicineRepository medicineRepository;
    private final VisitRepository visitRepository;
    private final PrescriptionClinicalContextValidator clinicalContextValidator;
    private final CurrentUserPort currentUserPort;
    private final ClockPort clockPort;

    @Override
    public ContraindicationCheckResult check(UUID medicalRecordId, List<UUID> medicineIds) {
        if (medicalRecordId == null) {
            throw new ValidationException("Medical record ID is required to check contraindications.");
        }
        UUID currentUserId = currentUserPort.getCurrentUserId();
        MedicalRecord medicalRecord = clinicalContextValidator.requireEditableRecordForDoctor(
                medicalRecordId,
                currentUserId
        );
        Visit visit = visitRepository.findById(medicalRecord.getVisitId())
                .orElseThrow(() -> new ValidationException("Visit not found for medical record: " + medicalRecordId));
        return checkByPatientId(visit.getPatientId(), medicineIds);
    }

    @Override
    public ContraindicationCheckResult checkByPatientId(UUID patientId, List<UUID> medicineIds) {
        if (patientId == null) {
            throw new ValidationException("Patient ID is required to check contraindications.");
        }
        List<UUID> distinctMedicineIds = distinctMedicineIds(medicineIds);
        if (distinctMedicineIds.isEmpty()) {
            return new ContraindicationCheckResult(List.of(), List.of());
        }

        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new ValidationException("Patient not found: " + patientId));
        List<Medicine> medicines = medicineRepository.findAllById(distinctMedicineIds);
        if (medicines.isEmpty()) {
            return new ContraindicationCheckResult(List.of(), List.of());
        }

        List<PatientChronicDisease> chronicDiseases =
                patientChronicDiseaseRepository.findByPatientIdAndActiveTrue(patientId);
        Set<UUID> chronicDiseaseCatalogIds = chronicDiseases.stream()
                .map(PatientChronicDisease::getDiagnosisCatalogId)
                .collect(java.util.stream.Collectors.toSet());

        List<String> ingredients = medicines.stream()
                .map(Medicine::getActiveIngredient)
                .filter(ingredient -> ingredient != null && !ingredient.isBlank())
                .distinct()
                .toList();

        List<ContraindicationRule> rules = ruleRepository.findActiveByMedicineIdsAndIngredients(
                distinctMedicineIds,
                ingredients
        );

        LocalDate today = clockPort.now().atZone(PatientMinorPolicy.CLINICAL_TIMEZONE).toLocalDate();
        Integer patientAge = patient.getDateOfBirth() == null
                ? null
                : Period.between(patient.getDateOfBirth(), today).getYears();

        List<ContraindicationWarningResult> warnings = new ArrayList<>();
        List<ContraindicationMissingDataResult> missingData = new ArrayList<>();

        for (Medicine medicine : medicines) {
            for (ContraindicationRule rule : rules) {
                if (!appliesTo(rule, medicine)) {
                    continue;
                }
                evaluate(rule, medicine, patient, patientAge, chronicDiseaseCatalogIds, warnings, missingData);
            }
        }

        warnings.sort(Comparator
                .comparingInt((ContraindicationWarningResult w) -> w.severity().ordinal())
                .reversed());

        return new ContraindicationCheckResult(List.copyOf(warnings), List.copyOf(missingData));
    }

    private boolean appliesTo(ContraindicationRule rule, Medicine medicine) {
        if (rule.getMedicineId() != null) {
            return rule.getMedicineId().equals(medicine.getId());
        }
        return rule.getActiveIngredient() != null
                && rule.getActiveIngredient().equalsIgnoreCase(medicine.getActiveIngredient());
    }

    private void evaluate(
            ContraindicationRule rule,
            Medicine medicine,
            Patient patient,
            Integer patientAge,
            Set<UUID> chronicDiseaseCatalogIds,
            List<ContraindicationWarningResult> warnings,
            List<ContraindicationMissingDataResult> missingData
    ) {
        switch (rule.getType()) {
            case AGE -> {
                if (patientAge == null) {
                    missingData.add(new ContraindicationMissingDataResult(
                            medicine.getId(),
                            medicine.getMedicineName(),
                            ContraindicationType.AGE,
                            "Patient date of birth is missing; age contraindication cannot be evaluated."));
                    return;
                }
                boolean belowMin = rule.getMinAgeYears() != null && patientAge < rule.getMinAgeYears();
                boolean aboveMax = rule.getMaxAgeYears() != null && patientAge > rule.getMaxAgeYears();
                if (belowMin || aboveMax) {
                    warnings.add(toWarning(rule, medicine, patient.getId()));
                }
            }
            case PREGNANCY -> {
                if (patient.getGender() != Gender.FEMALE) {
                    return;
                }
                if (patient.getPregnancyStatus() == null) {
                    missingData.add(new ContraindicationMissingDataResult(
                            medicine.getId(),
                            medicine.getMedicineName(),
                            ContraindicationType.PREGNANCY,
                            "Patient pregnancy status is missing; pregnancy contraindication cannot be evaluated."));
                    return;
                }
                if (patient.getPregnancyStatus() == PregnancyStatus.PREGNANT) {
                    warnings.add(toWarning(rule, medicine, patient.getId()));
                }
            }
            case DISEASE -> {
                if (chronicDiseaseCatalogIds.contains(rule.getDiagnosisCatalogId())) {
                    warnings.add(toWarning(rule, medicine, patient.getId()));
                }
            }
        }
    }

    private ContraindicationWarningResult toWarning(ContraindicationRule rule, Medicine medicine, UUID patientId) {
        return new ContraindicationWarningResult(
                patientId,
                rule.getId(),
                medicine.getId(),
                medicine.getMedicineName(),
                rule.getType(),
                rule.getSeverity(),
                rule.getMessage(),
                rule.getRecommendation()
        );
    }

    private List<UUID> distinctMedicineIds(List<UUID> medicineIds) {
        if (medicineIds == null || medicineIds.isEmpty()) {
            return List.of();
        }
        Set<UUID> distinct = new LinkedHashSet<>();
        for (UUID id : medicineIds) {
            if (id == null) {
                throw new ValidationException("Medicine ID is required.");
            }
            distinct.add(id);
        }
        return List.copyOf(distinct);
    }
}


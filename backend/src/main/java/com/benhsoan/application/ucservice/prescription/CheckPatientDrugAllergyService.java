package com.benhsoan.application.ucservice.prescription;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.domain.medicalrecord.MedicalRecord;
import com.benhsoan.domain.medicine.Medicine;
import com.benhsoan.domain.patient.PatientAllergy;
import com.benhsoan.domain.prescription.AllergyIngredientMatcher;
import com.benhsoan.domain.shared.exception.ValidationException;
import com.benhsoan.domain.visit.Visit;
import com.benhsoan.port.dto.result.PatientAllergyWarningResult;
import com.benhsoan.port.inbound.prescription.CheckPatientDrugAllergyUseCase;
import com.benhsoan.port.outbound.repository.medicalrecord.MedicalRecordRepository;
import com.benhsoan.port.outbound.repository.medicine.MedicineRepository;
import com.benhsoan.port.outbound.repository.patient.PatientAllergyRepository;
import com.benhsoan.port.outbound.repository.visit.VisitRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CheckPatientDrugAllergyService implements CheckPatientDrugAllergyUseCase {

    private final PatientAllergyRepository patientAllergyRepository;
    private final MedicineRepository medicineRepository;
    private final MedicalRecordRepository medicalRecordRepository;
    private final VisitRepository visitRepository;

    @Override
    public List<PatientAllergyWarningResult> check(UUID medicalRecordId, List<UUID> medicineIds) {
        if (medicalRecordId == null) {
            throw new ValidationException("Medical record ID is required to check allergy warnings.");
        }
        UUID patientId = resolvePatientId(medicalRecordId);
        return checkByPatientId(patientId, medicineIds);
    }

    @Override
    public List<PatientAllergyWarningResult> checkByPatientId(UUID patientId, List<UUID> medicineIds) {
        if (patientId == null) {
            throw new ValidationException("Patient ID is required to check allergy warnings.");
        }
        if (medicineIds == null || medicineIds.isEmpty()) {
            return List.of();
        }

        List<PatientAllergy> activeAllergies = patientAllergyRepository.findByPatientIdAndActiveTrue(patientId);
        if (activeAllergies.isEmpty()) {
            return List.of();
        }

        List<Medicine> medicines = medicineRepository.findAllById(medicineIds);
        if (medicines.isEmpty()) {
            return List.of();
        }

        List<PatientAllergyWarningResult> warnings = new ArrayList<>();
        for (Medicine medicine : medicines) {
            String activeIngredient = medicine.getActiveIngredient();
            for (PatientAllergy allergy : activeAllergies) {
                if (AllergyIngredientMatcher.matches(activeIngredient, allergy.getNormalizedAllergenName())) {
                    warnings.add(new PatientAllergyWarningResult(
                            allergy.getId(),
                            patientId,
                            medicine.getId(),
                            medicine.getMedicineName(),
                            activeIngredient,
                            allergy.getAllergenName(),
                            allergy.getSeverity(),
                            allergy.getReaction()
                    ));
                }
            }
        }

        return List.copyOf(warnings);
    }

    private UUID resolvePatientId(UUID medicalRecordId) {
        MedicalRecord medicalRecord = medicalRecordRepository.findById(medicalRecordId)
                .orElseThrow(() -> new ValidationException("Medical record not found: " + medicalRecordId));

        Visit visit = visitRepository.findById(medicalRecord.getVisitId())
                .orElseThrow(() -> new ValidationException("Visit not found for medical record: " + medicalRecordId));

        return visit.getPatientId();
    }
}

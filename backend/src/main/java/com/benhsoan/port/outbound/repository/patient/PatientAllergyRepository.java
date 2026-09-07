package com.benhsoan.port.outbound.repository.patient;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.benhsoan.domain.patient.PatientAllergy;

public interface PatientAllergyRepository {

    PatientAllergy save(PatientAllergy allergy);

    Optional<PatientAllergy> findById(UUID id);

    List<PatientAllergy> findByPatientIdAndActiveTrue(UUID patientId);

    boolean existsByPatientIdAndNormalizedAllergenNameAndActiveTrue(UUID patientId, String normalizedAllergenName);

    boolean existsByPatientIdAndNormalizedAllergenNameAndActiveTrueAndIdNot(
            UUID patientId,
            String normalizedAllergenName,
            UUID allergyId
    );
}

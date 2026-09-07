package com.benhsoan.port.inbound.patient;

import java.util.List;
import java.util.UUID;

import com.benhsoan.port.dto.result.patient.PatientAllergyResult;

public interface GetPatientAllergiesUseCase {
    List<PatientAllergyResult> getAllergies(UUID patientId);
}

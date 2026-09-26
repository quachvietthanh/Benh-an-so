package com.benhsoan.port.inbound.patient;

import java.util.List;
import java.util.UUID;

import com.benhsoan.port.dto.result.patient.PatientChronicDiseaseResult;

public interface GetPatientChronicDiseasesUseCase {
    List<PatientChronicDiseaseResult> getChronicDiseases(UUID patientId);
}

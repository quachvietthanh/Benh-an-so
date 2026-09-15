package com.benhsoan.port.inbound.patient;

import com.benhsoan.port.dto.command.patient.AddPatientChronicDiseaseCommand;
import com.benhsoan.port.dto.result.patient.PatientChronicDiseaseResult;

public interface AddPatientChronicDiseaseUseCase {
    PatientChronicDiseaseResult addChronicDisease(AddPatientChronicDiseaseCommand command);
}

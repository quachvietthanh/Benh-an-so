package com.benhsoan.port.inbound.patient;

import com.benhsoan.port.dto.command.patient.AddPatientAllergyCommand;
import com.benhsoan.port.dto.result.patient.PatientAllergyResult;

public interface AddPatientAllergyUseCase {
    PatientAllergyResult addAllergy(AddPatientAllergyCommand command);
}

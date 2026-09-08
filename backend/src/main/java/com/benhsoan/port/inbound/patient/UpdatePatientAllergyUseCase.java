package com.benhsoan.port.inbound.patient;

import com.benhsoan.port.dto.command.patient.UpdatePatientAllergyCommand;
import com.benhsoan.port.dto.result.patient.PatientAllergyResult;

public interface UpdatePatientAllergyUseCase {
    PatientAllergyResult updateAllergy(UpdatePatientAllergyCommand command);
}

package com.benhsoan.port.inbound.patient;

import com.benhsoan.port.dto.command.patient.DeletePatientAllergyCommand;

public interface DeletePatientAllergyUseCase {
    void deleteAllergy(DeletePatientAllergyCommand command);
}

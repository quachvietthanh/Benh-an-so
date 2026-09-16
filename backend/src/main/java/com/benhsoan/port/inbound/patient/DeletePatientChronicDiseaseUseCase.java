package com.benhsoan.port.inbound.patient;

import com.benhsoan.port.dto.command.patient.DeletePatientChronicDiseaseCommand;

public interface DeletePatientChronicDiseaseUseCase {
    void deleteChronicDisease(DeletePatientChronicDiseaseCommand command);
}

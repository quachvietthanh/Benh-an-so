package com.benhsoan.port.inbound.patient;

import com.benhsoan.port.dto.command.patient.DeletePatientFamilyHistoryCommand;

public interface DeletePatientFamilyHistoryUseCase {
    void deleteFamilyHistory(DeletePatientFamilyHistoryCommand command);
}

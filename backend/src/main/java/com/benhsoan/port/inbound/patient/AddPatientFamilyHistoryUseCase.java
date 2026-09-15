package com.benhsoan.port.inbound.patient;

import com.benhsoan.port.dto.command.patient.AddPatientFamilyHistoryCommand;
import com.benhsoan.port.dto.result.patient.PatientFamilyHistoryResult;

public interface AddPatientFamilyHistoryUseCase {
    PatientFamilyHistoryResult addFamilyHistory(AddPatientFamilyHistoryCommand command);
}

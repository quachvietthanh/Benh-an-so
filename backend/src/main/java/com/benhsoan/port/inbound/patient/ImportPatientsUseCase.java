package com.benhsoan.port.inbound.patient;

import com.benhsoan.port.dto.command.patient.ImportPatientsCommand;
import com.benhsoan.port.dto.result.patient.PatientImportResult;

public interface ImportPatientsUseCase {
    PatientImportResult importPatients(ImportPatientsCommand command);
}

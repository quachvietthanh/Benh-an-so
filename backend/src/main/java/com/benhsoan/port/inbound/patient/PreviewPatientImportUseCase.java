package com.benhsoan.port.inbound.patient;

import com.benhsoan.port.dto.command.patient.PreviewPatientImportCommand;
import com.benhsoan.port.dto.result.patient.PatientImportPreviewResult;

public interface PreviewPatientImportUseCase {
    PatientImportPreviewResult preview(PreviewPatientImportCommand command);
}

package com.benhsoan.port.inbound.patient;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.benhsoan.port.dto.result.patient.PatientImportLogResult;

public interface GetPatientImportLogsUseCase {
    Page<PatientImportLogResult> getLogs(Pageable pageable);
    PatientImportLogResult getLogById(UUID id);
}

package com.benhsoan.port.dto.result.patient;

import java.util.List;
import java.util.UUID;

public record PatientImportResult(
        UUID importLogId,
        String fileName,
        int totalRows,
        int successCount,
        int errorCount,
        int duplicateCount,
        List<String> createdPatientCodes,
        List<PatientImportRowErrorResult> errors
) {}

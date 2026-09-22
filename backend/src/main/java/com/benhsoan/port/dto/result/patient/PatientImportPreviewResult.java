package com.benhsoan.port.dto.result.patient;

import java.util.List;

public record PatientImportPreviewResult(
        String fileName,
        int totalRows,
        int validCount,
        int errorCount,
        int duplicateCount,
        List<PatientImportRowErrorResult> errors,
        List<SuspectedDuplicateResult> suspectedDuplicates
) {}

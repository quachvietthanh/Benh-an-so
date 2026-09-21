package com.benhsoan.adapter.inbound.rest.response.patient;

import java.util.List;

public record PatientImportPreviewResponse(
        String fileName,
        int totalRows,
        int validCount,
        int errorCount,
        int duplicateCount,
        List<PatientImportRowErrorResponse> errors,
        List<SuspectedDuplicateResponse> suspectedDuplicates
) {}

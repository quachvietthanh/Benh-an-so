package com.benhsoan.adapter.inbound.rest.response.patient;

import java.util.List;
import java.util.UUID;

public record PatientImportResultResponse(
        UUID importLogId,
        String fileName,
        int totalRows,
        int successCount,
        int errorCount,
        int duplicateCount,
        List<String> createdPatientCodes,
        List<PatientImportRowErrorResponse> errors
) {}

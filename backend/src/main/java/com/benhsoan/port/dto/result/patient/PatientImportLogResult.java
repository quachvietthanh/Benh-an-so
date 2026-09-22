package com.benhsoan.port.dto.result.patient;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.benhsoan.domain.patient.enums.ImportStatus;

public record PatientImportLogResult(
        UUID id,
        String fileName,
        long fileSize,
        int totalRows,
        int successRows,
        int errorRows,
        int duplicateRows,
        ImportStatus status,
        UUID importedBy,
        String importedByName,
        Instant createdAt,
        List<PatientImportRowErrorResult> errors
) {}

package com.benhsoan.port.dto.result.patient;

public record PatientImportRowErrorResult(
        int rowNumber,
        String errorField,
        String errorMessage,
        String rawData
) {}

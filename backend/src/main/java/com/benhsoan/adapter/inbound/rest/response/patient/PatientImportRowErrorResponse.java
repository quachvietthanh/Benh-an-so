package com.benhsoan.adapter.inbound.rest.response.patient;

public record PatientImportRowErrorResponse(
        int rowNumber,
        String errorField,
        String errorMessage,
        String rawData
) {}

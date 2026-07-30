package com.benhsoan.adapter.inbound.rest.request.medicalrecord;

import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;

public record RecordDiagnosisRequest(
        UUID diagnosisCatalogId,
        @NotBlank String primaryIcdCode,
        @NotBlank String primaryIcdName,
        List<SecondaryIcdItem> secondaryIcdCodes,
        String clinicalNotes
) {
    public record SecondaryIcdItem(String code, String name) {}
}

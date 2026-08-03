package com.benhsoan.adapter.inbound.rest.request.medicalrecord;

import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ReplaceMedicalRecordDiagnosesRequest(
        @NotNull @Valid DiagnosisRequest primaryDiagnosis,
        List<@Valid DiagnosisRequest> secondaryDiagnoses
) {

    public record DiagnosisRequest(
            @NotNull UUID diagnosisCatalogId,
            @NotBlank @Size(max = 30) String code,
            @NotBlank @Size(max = 255) String name,
            @Size(max = 5000) String note
    ) {
    }
}

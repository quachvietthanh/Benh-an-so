package com.benhsoan.adapter.inbound.rest.request.medicalrecord;

import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ReplaceMedicalRecordDiagnosesRequest(
        @NotNull @Valid PrimaryDiagnosisRequest primaryDiagnosis,
        List<@Valid SecondaryDiagnosisRequest> secondaryDiagnoses
) {

    public record PrimaryDiagnosisRequest(
            @NotNull UUID diagnosisCatalogId,
            @Size(max = 5000) String note
    ) {
    }

    public record SecondaryDiagnosisRequest(
            UUID diagnosisCatalogId,
            @Size(max = 255) String name,
            @Size(max = 5000) String note
    ) {

        @AssertTrue(message = "Secondary diagnosis must provide exactly one of diagnosisCatalogId or name.")
        public boolean hasCatalogOrFreeTextName() {
            return diagnosisCatalogId != null ^ (name != null && !name.isBlank());
        }
    }
}

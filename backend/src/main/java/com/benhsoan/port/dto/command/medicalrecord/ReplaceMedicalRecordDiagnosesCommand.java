package com.benhsoan.port.dto.command.medicalrecord;

import java.util.List;
import java.util.UUID;

public record ReplaceMedicalRecordDiagnosesCommand(
        PrimaryDiagnosisCommand primaryDiagnosis,
        List<SecondaryDiagnosisCommand> secondaryDiagnoses
) {

    public record PrimaryDiagnosisCommand(
            UUID diagnosisCatalogId,
            String note
    ) {
    }

    public record SecondaryDiagnosisCommand(
            UUID diagnosisCatalogId,
            String name,
            String note
    ) {
    }
}

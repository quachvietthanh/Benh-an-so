package com.benhsoan.port.dto.command.medicalrecord;

import java.util.List;
import java.util.UUID;

public record ReplaceMedicalRecordDiagnosesCommand(
        DiagnosisCommand primaryDiagnosis,
        List<DiagnosisCommand> secondaryDiagnoses
) {

    public record DiagnosisCommand(
            UUID diagnosisCatalogId,
            String code,
            String name,
            String note
    ) {
    }
}

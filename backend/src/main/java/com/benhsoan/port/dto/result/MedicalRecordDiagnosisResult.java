package com.benhsoan.port.dto.result;

import java.time.Instant;
import java.util.UUID;

import com.benhsoan.domain.medicalrecord.enums.DiagnosisType;

public record MedicalRecordDiagnosisResult(
        UUID id,
        UUID medicalRecordId,
        UUID diagnosisCatalogId,
        String diagnosisCode,
        String diagnosisName,
        DiagnosisType diagnosisType,
        String note,
        UUID diagnosedBy,
        Instant diagnosedAt
) {
    public MedicalRecordDiagnosisResult(
            UUID id,
            UUID medicalRecordId,
            String diagnosisCode,
            String diagnosisName,
            DiagnosisType diagnosisType,
            String note,
            UUID diagnosedBy,
            Instant diagnosedAt
    ) {
        this(id, medicalRecordId, null, diagnosisCode, diagnosisName, diagnosisType, note, diagnosedBy, diagnosedAt);
    }
}

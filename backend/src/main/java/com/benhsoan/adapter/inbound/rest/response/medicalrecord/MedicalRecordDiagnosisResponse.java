package com.benhsoan.adapter.inbound.rest.response.medicalrecord;

import java.time.Instant;
import java.util.UUID;

import com.benhsoan.domain.medicalrecord.enums.DiagnosisType;

public record MedicalRecordDiagnosisResponse(
        UUID id,
        UUID medicalRecordId,
        String diagnosisCode,
        String diagnosisName,
        DiagnosisType diagnosisType,
        String note,
        UUID diagnosedBy,
        Instant diagnosedAt
) {
}

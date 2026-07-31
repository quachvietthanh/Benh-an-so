package com.benhsoan.port.dto.result;

import java.time.Instant;
import java.util.UUID;

import com.benhsoan.domain.medicalrecord.enums.DiagnosisType;

/**
 * Read model for a single diagnosis attached to a medical record.
 */
public record MedicalRecordDiagnosisResult(
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

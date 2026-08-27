package com.benhsoan.port.dto.result.patient;

import java.time.Instant;
import java.util.UUID;

public record PatientMedicalHistorySummaryResult(
        UUID visitId,
        Instant visitAt,
        String doctorName,
        String specialtyName,
        String diagnosisSummary,
        int prescriptionCount
) {
}

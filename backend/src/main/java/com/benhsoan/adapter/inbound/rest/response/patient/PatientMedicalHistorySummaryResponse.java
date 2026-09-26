package com.benhsoan.adapter.inbound.rest.response.patient;

import java.time.Instant;
import java.util.UUID;

public record PatientMedicalHistorySummaryResponse(
        UUID visitId,
        Instant visitAt,
        String doctorName,
        String specialtyName,
        String diagnosisSummary,
        int prescriptionCount
) {
}

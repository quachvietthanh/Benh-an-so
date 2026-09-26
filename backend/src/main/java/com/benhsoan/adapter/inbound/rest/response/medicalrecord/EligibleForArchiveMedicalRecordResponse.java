package com.benhsoan.adapter.inbound.rest.response.medicalrecord;

import java.time.Instant;
import java.util.UUID;

public record EligibleForArchiveMedicalRecordResponse(
        UUID medicalRecordId,
        UUID visitId,
        String visitCode,
        UUID patientId,
        String patientCode,
        String patientFullName,
        UUID doctorId,
        String doctorFullName,
        String specialtyName,
        String status,
        Instant completedAt,
        Instant signedAt
) {
}

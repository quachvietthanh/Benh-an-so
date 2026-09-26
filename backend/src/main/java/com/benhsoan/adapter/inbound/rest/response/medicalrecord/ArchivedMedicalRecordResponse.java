package com.benhsoan.adapter.inbound.rest.response.medicalrecord;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record ArchivedMedicalRecordResponse(
        UUID medicalRecordId,
        UUID visitId,
        String visitCode,
        UUID patientId,
        String patientCode,
        String patientFullName,
        String patientPhone,
        UUID doctorId,
        String doctorFullName,
        String conclusion,
        LocalDate revisitDate,
        String status,
        Instant completedAt,
        Instant signedAt,
        Instant archivedAt,
        UUID archivedBy
) {
}

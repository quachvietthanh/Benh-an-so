package com.benhsoan.persistence.jpaRepository.medicalrecord;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import com.benhsoan.domain.medicalrecord.enums.MedicalRecordStatus;

public record ArchivedMedicalRecordProjection(
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
        MedicalRecordStatus status,
        Instant completedAt,
        Instant signedAt,
        Instant archivedAt,
        UUID archivedBy
) {
}

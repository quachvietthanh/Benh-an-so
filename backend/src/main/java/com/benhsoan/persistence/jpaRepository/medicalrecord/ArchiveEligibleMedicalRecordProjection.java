package com.benhsoan.persistence.jpaRepository.medicalrecord;

import java.time.Instant;
import java.util.UUID;

import com.benhsoan.domain.medicalrecord.enums.MedicalRecordStatus;

public record ArchiveEligibleMedicalRecordProjection(
        UUID medicalRecordId,
        UUID visitId,
        String visitCode,
        UUID patientId,
        String patientCode,
        String patientFullName,
        UUID doctorId,
        String doctorFullName,
        String specialtyName,
        MedicalRecordStatus status,
        Instant completedAt,
        Instant signedAt
) {
}

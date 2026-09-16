package com.benhsoan.persistence.jpaRepository.medicalrecord;

import java.time.Instant;
import java.util.UUID;

import com.benhsoan.domain.medicalrecord.enums.MedicalRecordStatus;

public record OverdueMedicalRecordProjection(
        UUID medicalRecordId,
        MedicalRecordStatus status,
        UUID visitId,
        String visitCode,
        Instant visitCompletedAt,
        UUID patientId,
        String patientCode,
        String patientFullName,
        UUID doctorId,
        String doctorFullName,
        String doctorEmail,
        String doctorPhone,
        Long reminderCount,
        Instant lastRemindedAt
) {
}

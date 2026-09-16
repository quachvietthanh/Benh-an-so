package com.benhsoan.port.dto.result;

import java.time.Instant;
import java.util.UUID;

import com.benhsoan.domain.medicalrecord.enums.MedicalRecordStatus;

public record OverdueMedicalRecordResult(
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
        int signingDeadlineHours,
        Instant deadlineAt,
        long overdueHours,
        long reminderCount,
        Instant lastRemindedAt
) {
}

package com.benhsoan.adapter.inbound.rest.response.medicalrecord;

import java.time.Instant;
import java.util.UUID;

import com.benhsoan.domain.medicalrecord.enums.MedicalRecordStatus;

public record OverdueMedicalRecordResponse(
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

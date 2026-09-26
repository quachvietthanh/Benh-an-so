package com.benhsoan.port.outbound.notification;

import java.time.Instant;
import java.util.UUID;

public record SigningReminderMessage(
        UUID medicalRecordId,
        String visitCode,
        UUID doctorId,
        String doctorFullName,
        String doctorEmail,
        String doctorPhone,
        String patientFullName,
        long overdueHours,
        Instant deadlineAt,
        String customNotes
) {
}

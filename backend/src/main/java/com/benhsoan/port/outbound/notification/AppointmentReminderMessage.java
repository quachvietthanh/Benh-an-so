package com.benhsoan.port.outbound.notification;

import java.time.Instant;
import java.util.UUID;

public record AppointmentReminderMessage(
        UUID appointmentId,
        String appointmentCode,
        String patientName,
        String doctorName,
        Instant startTime,
        String content
) {
}

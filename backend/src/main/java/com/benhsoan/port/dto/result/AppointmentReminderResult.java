package com.benhsoan.port.dto.result;

public record AppointmentReminderResult(
        String status,
        String message
) {
    public static AppointmentReminderResult sent() {
        return new AppointmentReminderResult("SENT", "Appointment reminder was sent.");
    }

    public static AppointmentReminderResult skipped(String message) {
        return new AppointmentReminderResult("SKIPPED", message);
    }

    public static AppointmentReminderResult failed(String message) {
        return new AppointmentReminderResult("FAILED", message);
    }
}

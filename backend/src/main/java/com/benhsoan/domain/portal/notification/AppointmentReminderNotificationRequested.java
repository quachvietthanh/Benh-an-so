package com.benhsoan.domain.portal.notification;

import java.time.Instant;

import com.benhsoan.domain.appointment.Appointment;
import com.benhsoan.domain.auth.User;
import com.benhsoan.domain.patient.Patient;

/**
 * NCL-14-CN-008: domain event emitted after a due appointment reminder has been
 * successfully sent, requesting the creation of the matching patient-portal
 * notification. Delivered after the core transaction commits so a notification
 * persistence failure can never roll back the reminder-processing transaction.
 */
public record AppointmentReminderNotificationRequested(
        Appointment appointment,
        Patient patient,
        User doctor,
        Instant now
) {
}

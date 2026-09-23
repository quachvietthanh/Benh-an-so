package com.benhsoan.domain.portal.notification;

import java.time.Instant;

import com.benhsoan.domain.appointment.Appointment;
import com.benhsoan.domain.appointment.AppointmentRescheduleLog;

/**
 * NCL-14-CN-008: domain event emitted after an appointment reschedule has
 * committed, requesting the creation of the matching patient-portal
 * notification. Delivered after commit so the notification's
 * {@code reschedule_log_id} foreign key always references a committed row.
 */
public record AppointmentChangedNotificationRequested(
        Appointment appointment,
        AppointmentRescheduleLog rescheduleLog,
        Instant now
) {
}

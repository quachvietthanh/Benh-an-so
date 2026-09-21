package com.benhsoan.port.outbound.repository.reporting;

import com.benhsoan.domain.appointment.enums.AppointmentStatus;

/**
 * Number of appointments for a single {@link AppointmentStatus} inside a
 * reporting window. The status is the authoritative
 * {@link AppointmentStatus} enum value stored on {@code appointments.status}.
 */
public record AppointmentStatusCountSummary(
        AppointmentStatus status,
        long count
) {
}

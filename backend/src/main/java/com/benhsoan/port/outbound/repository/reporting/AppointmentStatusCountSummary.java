package com.benhsoan.port.outbound.repository.reporting;

import com.benhsoan.domain.appointment.enums.AppointmentStatus;

/**
 * Number of appointments for a single {@link AppointmentStatus} and booking
 * channel inside a reporting window.
 *
 * <p>{@code bookingChannel} is the normalized channel label:
 * {@code "ONLINE_PORTAL"} for patient-portal bookings and
 * {@code "RECEPTION_COUNTER"} for at-counter/legacy bookings (whose
 * {@code booking_channel} column is {@code NULL}).</p>
 */
public record AppointmentStatusCountSummary(
        String bookingChannel,
        AppointmentStatus status,
        long count
) {
}

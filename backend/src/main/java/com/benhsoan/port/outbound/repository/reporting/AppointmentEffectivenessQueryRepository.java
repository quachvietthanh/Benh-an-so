package com.benhsoan.port.outbound.repository.reporting;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Read-side port for the appointment effectiveness report (NCL-08-CN-008).
 *
 * <p>Aggregates appointment counts by booking channel and status for a half-open
 * time window {@code [fromInclusive, toExclusive)} keyed on
 * {@code appointments.start_time} (the scheduled occurrence time, not the
 * creation time).</p>
 */
public interface AppointmentEffectivenessQueryRepository {

    /**
     * Returns one row per (booking channel, appointment status) pair present in
     * the window.
     *
     * @param fromInclusive  inclusive lower bound on {@code start_time}
     * @param toExclusive    exclusive upper bound on {@code start_time}
     * @param doctorId       optional doctor filter ({@code null} means all doctors)
     * @param bookingChannel optional channel filter; {@code null} returns all
     *                       channels broken down, {@code "ONLINE_PORTAL"} matches
     *                       portal bookings and {@code "RECEPTION_COUNTER"} matches
     *                       at-counter bookings (stored as {@code NULL}
     *                       {@code booking_channel})
     */
    List<AppointmentStatusCountSummary> findStatusCounts(
            Instant fromInclusive,
            Instant toExclusive,
            UUID doctorId,
            String bookingChannel
    );
}

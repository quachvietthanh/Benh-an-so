package com.benhsoan.domain.appointment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.benhsoan.domain.appointment.enums.AppointmentStatus;
import com.benhsoan.domain.appointment.exception.AppointmentInvalidStatusException;

class AppointmentTest {

    @Test
    void doesNotAllowNoShowAppointmentToBeCancelled() {
        Appointment appointment = Appointment.restore(
                UUID.randomUUID(),
                "APT-001",
                UUID.randomUUID(),
                UUID.randomUUID(),
                Instant.now().minusSeconds(3600),
                Instant.now().minusSeconds(1800),
                AppointmentStatus.NO_SHOW,
                "Consultation",
                null,
                null,
                null,
                UUID.randomUUID(),
                Instant.now().minusSeconds(7200)
        );

        assertThrows(AppointmentInvalidStatusException.class,
                () -> appointment.cancel("Patient requested cancellation"));

        assertEquals(AppointmentStatus.NO_SHOW, appointment.getStatus());
        assertNull(appointment.getCancelReason());
    }
}

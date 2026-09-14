package com.benhsoan.domain.appointment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.benhsoan.domain.appointment.enums.AppointmentStatus;
import com.benhsoan.domain.appointment.exception.AppointmentAlreadyCancelledException;
import com.benhsoan.domain.appointment.exception.AppointmentAlreadyCompletedException;
import com.benhsoan.domain.appointment.exception.AppointmentInvalidStatusException;
import com.benhsoan.domain.appointment.exception.AppointmentPastCutoffException;
import com.benhsoan.domain.appointment.exception.AppointmentTimeInPastException;
import com.benhsoan.domain.shared.exception.ValidationException;

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

    @Test
    void doesNotAllowCheckedInAppointmentToBeMarkedNoShow() {
        Instant now = Instant.parse("2026-08-02T03:00:00Z");
        Appointment appointment = Appointment.restore(
                UUID.randomUUID(), "APT-002", UUID.randomUUID(), UUID.randomUUID(),
                now.minusSeconds(3600), now.minusSeconds(1800), AppointmentStatus.CHECKED_IN,
                "Consultation", null, now.minusSeconds(3500), null, UUID.randomUUID(), now.minusSeconds(7200));

        assertThrows(AppointmentInvalidStatusException.class, () -> appointment.markNoShow(now));
        assertEquals(AppointmentStatus.CHECKED_IN, appointment.getStatus());
    }

    @Test
    void reschedulesSuccessfullyWhenStatusIsScheduledAndFuture() {
        Instant now = Instant.parse("2026-09-14T08:00:00Z");
        UUID oldDoctorId = UUID.randomUUID();
        UUID newDoctorId = UUID.randomUUID();
        Appointment appointment = Appointment.restore(
                UUID.randomUUID(), "APT-003", UUID.randomUUID(), oldDoctorId,
                now.plusSeconds(3600), now.plusSeconds(5400), AppointmentStatus.SCHEDULED,
                "Lý do ban đầu", null, null, null, UUID.randomUUID(), now.minusSeconds(3600));

        Instant newStartTime = now.plusSeconds(7200);
        Instant newEndTime = now.plusSeconds(9000);
        appointment.reschedule(newDoctorId, newStartTime, newEndTime, "Bệnh nhân báo trễ", now);

        assertEquals(newDoctorId, appointment.getDoctorId());
        assertEquals(newStartTime, appointment.getStartTime());
        assertEquals(newEndTime, appointment.getEndTime());
        assertEquals("Lý do ban đầu", appointment.getReason());
    }

    @Test
    void reschedulesSuccessfullyWhenStatusIsConfirmed() {
        Instant now = Instant.parse("2026-09-14T08:00:00Z");
        UUID doctorId = UUID.randomUUID();
        Appointment appointment = Appointment.restore(
                UUID.randomUUID(), "APT-004", UUID.randomUUID(), doctorId,
                now.plusSeconds(3600), now.plusSeconds(5400), AppointmentStatus.CONFIRMED,
                "Lý do ban đầu", null, null, null, UUID.randomUUID(), now.minusSeconds(3600));

        Instant newStartTime = now.plusSeconds(7200);
        Instant newEndTime = now.plusSeconds(9000);
        appointment.reschedule(doctorId, newStartTime, newEndTime, now);

        assertEquals(newStartTime, appointment.getStartTime());
        assertEquals("Lý do ban đầu", appointment.getReason());
    }

    @Test
    void doesNotAllowRescheduleWhenCurrentAppointmentIsPast() {
        Instant now = Instant.parse("2026-09-14T08:00:00Z");
        Appointment appointment = Appointment.restore(
                UUID.randomUUID(), "APT-005", UUID.randomUUID(), UUID.randomUUID(),
                now.minusSeconds(1800), now.minusSeconds(600), AppointmentStatus.SCHEDULED,
                "Lý do ban đầu", null, null, null, UUID.randomUUID(), now.minusSeconds(7200));

        assertThrows(AppointmentPastCutoffException.class,
                () -> appointment.reschedule(UUID.randomUUID(), now.plusSeconds(3600), now.plusSeconds(5400), "Dời lịch", now));
    }

    @Test
    void doesNotAllowRescheduleWhenStatusIsInvalid() {
        Instant now = Instant.parse("2026-09-14T08:00:00Z");

        // NO_SHOW
        Appointment noShowAppointment = Appointment.restore(
                UUID.randomUUID(), "APT-006", UUID.randomUUID(), UUID.randomUUID(),
                now.plusSeconds(3600), now.plusSeconds(5400), AppointmentStatus.NO_SHOW,
                "Lý do ban đầu", null, null, null, UUID.randomUUID(), now.minusSeconds(7200));
        assertThrows(AppointmentInvalidStatusException.class,
                () -> noShowAppointment.reschedule(UUID.randomUUID(), now.plusSeconds(7200), now.plusSeconds(9000), "Dời lịch", now));

        // CANCELLED
        Appointment cancelledAppointment = Appointment.restore(
                UUID.randomUUID(), "APT-007", UUID.randomUUID(), UUID.randomUUID(),
                now.plusSeconds(3600), now.plusSeconds(5400), AppointmentStatus.CANCELLED,
                "Lý do ban đầu", "Hủy", null, null, UUID.randomUUID(), now.minusSeconds(7200));
        assertThrows(AppointmentAlreadyCancelledException.class,
                () -> cancelledAppointment.reschedule(UUID.randomUUID(), now.plusSeconds(7200), now.plusSeconds(9000), "Dời lịch", now));

        // COMPLETED
        Appointment completedAppointment = Appointment.restore(
                UUID.randomUUID(), "APT-008", UUID.randomUUID(), UUID.randomUUID(),
                now.plusSeconds(3600), now.plusSeconds(5400), AppointmentStatus.COMPLETED,
                "Lý do ban đầu", null, null, now.minusSeconds(100), UUID.randomUUID(), now.minusSeconds(7200));
        assertThrows(AppointmentAlreadyCompletedException.class,
                () -> completedAppointment.reschedule(UUID.randomUUID(), now.plusSeconds(7200), now.plusSeconds(9000), "Dời lịch", now));
    }

    @Test
    void doesNotAllowRescheduleWhenNewTimesAreInvalid() {
        Instant now = Instant.parse("2026-09-14T08:00:00Z");
        Appointment appointment = Appointment.restore(
                UUID.randomUUID(), "APT-009", UUID.randomUUID(), UUID.randomUUID(),
                now.plusSeconds(3600), now.plusSeconds(5400), AppointmentStatus.SCHEDULED,
                "Lý do ban đầu", null, null, null, UUID.randomUUID(), now.minusSeconds(3600));

        // End time before start time
        assertThrows(ValidationException.class,
                () -> appointment.reschedule(UUID.randomUUID(), now.plusSeconds(7200), now.plusSeconds(5400), "Dời lịch", now));

        // New start time in the past
        assertThrows(AppointmentTimeInPastException.class,
                () -> appointment.reschedule(UUID.randomUUID(), now.minusSeconds(100), now.plusSeconds(1800), "Dời lịch", now));
    }
}

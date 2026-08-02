package com.benhsoan.application.ucservice.queue;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.benhsoan.domain.appointment.Appointment;
import com.benhsoan.domain.appointment.enums.AppointmentStatus;
import com.benhsoan.domain.queue.exception.CheckInConflictException;
import com.benhsoan.port.dto.command.queue.CheckInAppointmentCommand;
import com.benhsoan.port.outbound.repository.crudRepository.appointment.AppointmentRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.benhsoan.port.outbound.time.ClockPort;

class CheckInAppointmentServiceTest {

    @Test
    void rejectsDifferentScheduledDateBeforeMutatingAppointment() {
        Instant checkedInAt = Instant.parse("2026-08-02T02:00:00Z");
        Instant appointmentStart = Instant.parse("2026-08-03T02:00:00Z");
        UUID appointmentId = UUID.randomUUID();
        Appointment appointment = Appointment.restore(appointmentId, "AP000300", UUID.randomUUID(), UUID.randomUUID(),
                appointmentStart, appointmentStart.plusSeconds(1800), AppointmentStatus.SCHEDULED, "Consultation",
                null, null, null, UUID.randomUUID(), checkedInAt.minusSeconds(3600));
        AppointmentRepository appointmentRepository = mock(AppointmentRepository.class);
        QueueCheckInCoordinator coordinator = mock(QueueCheckInCoordinator.class);
        CurrentUserPort currentUserPort = mock(CurrentUserPort.class);
        ClockPort clockPort = mock(ClockPort.class);
        when(currentUserPort.hasRole("RECEPTIONIST")).thenReturn(true);
        when(appointmentRepository.findByIdForUpdate(appointmentId)).thenReturn(Optional.of(appointment));
        when(clockPort.now()).thenReturn(checkedInAt);
        doThrow(new CheckInConflictException("Appointment can only be checked in on its scheduled date."))
                .when(coordinator).requireAppointmentOnQueueDate(appointmentStart, checkedInAt);

        CheckInAppointmentService service = new CheckInAppointmentService(
                appointmentRepository, coordinator, currentUserPort, clockPort);

        assertThrows(CheckInConflictException.class,
                () -> service.checkIn(new CheckInAppointmentCommand(appointmentId)));
        verify(appointmentRepository, never()).save(any());
        verify(coordinator, never()).checkIn(any(), any(), any(), any(), any(), any(), any(), any());
    }
}

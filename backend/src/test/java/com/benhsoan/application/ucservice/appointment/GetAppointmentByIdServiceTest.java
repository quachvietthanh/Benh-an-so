package com.benhsoan.application.ucservice.appointment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.benhsoan.domain.appointment.Appointment;
import com.benhsoan.domain.appointment.enums.AppointmentStatus;
import com.benhsoan.domain.appointment.exception.AppointmentNotFoundException;
import com.benhsoan.port.dto.result.AppointmentResult;
import com.benhsoan.port.outbound.repository.appointment.AppointmentRepository;

class GetAppointmentByIdServiceTest {

    @Test
    void returnsMappedAppointmentWhenFound() {
        UUID appointmentId = UUID.randomUUID();
        Appointment appointment = Appointment.restore(
                appointmentId,
                "APT000200",
                UUID.randomUUID(),
                UUID.randomUUID(),
                Instant.parse("2026-08-12T09:00:00Z"),
                Instant.parse("2026-08-12T09:30:00Z"),
                AppointmentStatus.SCHEDULED,
                "Tai kham",
                null,
                null,
                null,
                UUID.randomUUID(),
                Instant.parse("2026-08-09T02:00:00Z")
        );
        AppointmentRepository appointmentRepository = mock(AppointmentRepository.class);
        when(appointmentRepository.findById(appointmentId)).thenReturn(Optional.of(appointment));

        AppointmentResult result = new GetAppointmentByIdService(
                appointmentRepository,
                new AppointmentResultMapper()
        ).getById(appointmentId);

        assertEquals("APT000200", result.appointmentCode());
        assertEquals(AppointmentStatus.SCHEDULED, result.status());
    }

    @Test
    void throwsNotFoundWhenAppointmentDoesNotExist() {
        UUID appointmentId = UUID.randomUUID();
        AppointmentRepository appointmentRepository = mock(AppointmentRepository.class);
        when(appointmentRepository.findById(appointmentId)).thenReturn(Optional.empty());

        assertThrows(AppointmentNotFoundException.class, () -> new GetAppointmentByIdService(
                appointmentRepository,
                new AppointmentResultMapper()
        ).getById(appointmentId));
    }
}

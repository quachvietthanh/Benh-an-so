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
import com.benhsoan.domain.auth.User;
import com.benhsoan.port.dto.result.AppointmentResult;
import com.benhsoan.port.outbound.repository.appointment.AppointmentRepository;
import com.benhsoan.port.outbound.repository.auth.UserRepository;

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
        AppointmentRescheduleHistoryAssembler historyAssembler = mock(AppointmentRescheduleHistoryAssembler.class);
        UserRepository userRepository = mock(UserRepository.class);
        when(appointmentRepository.findById(appointmentId)).thenReturn(Optional.of(appointment));
        when(historyAssembler.getHistoriesForAppointment(appointmentId)).thenReturn(java.util.List.of());

        AppointmentResult result = new GetAppointmentByIdService(
                appointmentRepository,
                new AppointmentResultMapper(),
                historyAssembler,
                userRepository
        ).getById(appointmentId);

        assertEquals("APT000200", result.appointmentCode());
        assertEquals(AppointmentStatus.SCHEDULED, result.status());
    }

    @Test
    void returnsMappedAppointmentWithConfirmedByNameWhenConfirmed_Finding1() {
        UUID appointmentId = UUID.randomUUID();
        UUID confirmedBy = UUID.randomUUID();
        Instant confirmedAt = Instant.parse("2026-08-10T10:00:00Z");

        Appointment appointment = Appointment.restore(
                appointmentId,
                "APT000201",
                UUID.randomUUID(),
                UUID.randomUUID(),
                Instant.parse("2026-08-12T09:00:00Z"),
                Instant.parse("2026-08-12T09:30:00Z"),
                AppointmentStatus.CONFIRMED,
                "Tai kham",
                null,
                null,
                null,
                UUID.randomUUID(),
                Instant.parse("2026-08-09T02:00:00Z"),
                null,
                confirmedAt,
                confirmedBy
        );

        AppointmentRepository appointmentRepository = mock(AppointmentRepository.class);
        AppointmentRescheduleHistoryAssembler historyAssembler = mock(AppointmentRescheduleHistoryAssembler.class);
        UserRepository userRepository = mock(UserRepository.class);

        User mockUser = mock(User.class);
        when(mockUser.getFullName()).thenReturn("Le Tan Nguyen Van A");
        when(userRepository.findById(confirmedBy)).thenReturn(Optional.of(mockUser));

        when(appointmentRepository.findById(appointmentId)).thenReturn(Optional.of(appointment));
        when(historyAssembler.getHistoriesForAppointment(appointmentId)).thenReturn(java.util.List.of());

        AppointmentResult result = new GetAppointmentByIdService(
                appointmentRepository,
                new AppointmentResultMapper(),
                historyAssembler,
                userRepository
        ).getById(appointmentId);

        assertEquals("APT000201", result.appointmentCode());
        assertEquals(AppointmentStatus.CONFIRMED, result.status());
        assertEquals(confirmedBy, result.confirmedBy());
        assertEquals(confirmedAt, result.confirmedAt());
        assertEquals("Le Tan Nguyen Van A", result.confirmedByName());
    }

    @Test
    void returnsUnknownWhenConfirmedUserNotFound() {
        UUID appointmentId = UUID.randomUUID();
        UUID confirmedBy = UUID.randomUUID();

        Appointment appointment = Appointment.restore(
                appointmentId,
                "APT000202",
                UUID.randomUUID(),
                UUID.randomUUID(),
                Instant.parse("2026-08-12T09:00:00Z"),
                Instant.parse("2026-08-12T09:30:00Z"),
                AppointmentStatus.CONFIRMED,
                "Tai kham",
                null,
                null,
                null,
                UUID.randomUUID(),
                Instant.parse("2026-08-09T02:00:00Z"),
                null,
                Instant.now(),
                confirmedBy
        );

        AppointmentRepository appointmentRepository = mock(AppointmentRepository.class);
        AppointmentRescheduleHistoryAssembler historyAssembler = mock(AppointmentRescheduleHistoryAssembler.class);
        UserRepository userRepository = mock(UserRepository.class);

        when(userRepository.findById(confirmedBy)).thenReturn(Optional.empty());
        when(appointmentRepository.findById(appointmentId)).thenReturn(Optional.of(appointment));
        when(historyAssembler.getHistoriesForAppointment(appointmentId)).thenReturn(java.util.List.of());

        AppointmentResult result = new GetAppointmentByIdService(
                appointmentRepository,
                new AppointmentResultMapper(),
                historyAssembler,
                userRepository
        ).getById(appointmentId);

        assertEquals("Unknown", result.confirmedByName());
    }

    @Test
    void throwsNotFoundWhenAppointmentDoesNotExist() {
        UUID appointmentId = UUID.randomUUID();
        AppointmentRepository appointmentRepository = mock(AppointmentRepository.class);
        AppointmentRescheduleHistoryAssembler historyAssembler = mock(AppointmentRescheduleHistoryAssembler.class);
        UserRepository userRepository = mock(UserRepository.class);
        when(appointmentRepository.findById(appointmentId)).thenReturn(Optional.empty());

        assertThrows(AppointmentNotFoundException.class, () -> new GetAppointmentByIdService(
                appointmentRepository,
                new AppointmentResultMapper(),
                historyAssembler,
                userRepository
        ).getById(appointmentId));
    }
}

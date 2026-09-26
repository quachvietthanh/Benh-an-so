package com.benhsoan.application.ucservice.appointment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import com.benhsoan.domain.appointment.Appointment;
import com.benhsoan.domain.appointment.enums.AppointmentStatus;
import com.benhsoan.port.dto.command.appointment.SearchAppointmentCommand;
import com.benhsoan.port.dto.result.AppointmentResult;
import com.benhsoan.port.outbound.repository.appointment.AppointmentRepository;

class SearchAppointmentsServiceTest {

    @Test
    void mapsRepositoryPageToResultPage() {
        AppointmentRepository appointmentRepository = mock(AppointmentRepository.class);
        SearchAppointmentCommand command = new SearchAppointmentCommand(
                null,
                null,
                AppointmentStatus.SCHEDULED,
                Instant.parse("2099-08-10T00:00:00Z"),
                Instant.parse("2099-08-11T00:00:00Z"),
                PageRequest.of(0, 20)
        );
        Appointment appointment = Appointment.restore(
                UUID.randomUUID(),
                "APT000201",
                UUID.randomUUID(),
                UUID.randomUUID(),
                Instant.parse("2099-08-10T09:00:00Z"),
                Instant.parse("2099-08-10T09:30:00Z"),
                AppointmentStatus.SCHEDULED,
                "Tai kham",
                null,
                null,
                null,
                UUID.randomUUID(),
                Instant.parse("2026-08-09T03:00:00Z")
        );
        when(appointmentRepository.search(command)).thenReturn(new PageImpl<>(java.util.List.of(appointment)));

        Page<AppointmentResult> result = new SearchAppointmentsService(
                appointmentRepository,
                new AppointmentResultMapper()
        ).search(command);

        assertEquals(1, result.getTotalElements());
        assertEquals("APT000201", result.getContent().getFirst().appointmentCode());
        assertEquals(AppointmentStatus.SCHEDULED, result.getContent().getFirst().status());
    }
}

package com.benhsoan.application.ucservice.appointment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import com.benhsoan.domain.appointment.Appointment;
import com.benhsoan.domain.appointment.enums.AppointmentStatus;
import com.benhsoan.port.dto.command.appointment.GetOverdueAppointmentsCommand;
import com.benhsoan.port.dto.result.AppointmentResult;
import com.benhsoan.port.outbound.repository.appointment.AppointmentRepository;

class GetOverdueAppointmentsServiceTest {

    @Test
    void returnsMappedOverdueAppointments() {
        AppointmentRepository appointmentRepository = mock(AppointmentRepository.class);
        Appointment appointment = Appointment.restore(
                UUID.randomUUID(),
                "APT000220",
                UUID.randomUUID(),
                UUID.randomUUID(),
                Instant.parse("2026-08-09T00:30:00Z"),
                Instant.parse("2026-08-09T01:00:00Z"),
                AppointmentStatus.SCHEDULED,
                "Kham qua hen",
                null,
                null,
                null,
                UUID.randomUUID(),
                Instant.parse("2026-08-08T02:00:00Z")
        );
        when(appointmentRepository.findOverdue(any(), any()))
                .thenReturn(new PageImpl<>(List.of(appointment)));

        Page<AppointmentResult> result = new GetOverdueAppointmentsService(
                appointmentRepository,
                new AppointmentResultMapper()
        ).execute(GetOverdueAppointmentsCommand.builder()
                .pageable(PageRequest.of(0, 20))
                .build());

        assertEquals(1, result.getTotalElements());
        assertEquals("APT000220", result.getContent().getFirst().appointmentCode());
        verify(appointmentRepository).findOverdue(any(), any());
    }
}

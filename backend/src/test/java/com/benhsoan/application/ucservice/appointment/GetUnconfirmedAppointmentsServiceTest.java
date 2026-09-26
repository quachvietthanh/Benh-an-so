package com.benhsoan.application.ucservice.appointment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import com.benhsoan.domain.appointment.Appointment;
import com.benhsoan.domain.appointment.enums.AppointmentStatus;
import com.benhsoan.port.dto.result.AppointmentResult;
import com.benhsoan.port.outbound.repository.appointment.AppointmentRepository;
import com.benhsoan.port.outbound.time.ClockPort;

class GetUnconfirmedAppointmentsServiceTest {

    private AppointmentRepository appointmentRepository;
    private AppointmentResultMapper resultMapper;
    private ClockPort clockPort;
    private GetUnconfirmedAppointmentsService service;

    private final Instant now = Instant.parse("2026-09-14T08:00:00Z");

    @BeforeEach
    void setUp() {
        appointmentRepository = mock(AppointmentRepository.class);
        resultMapper = new AppointmentResultMapper();
        clockPort = mock(ClockPort.class);

        when(clockPort.now()).thenReturn(now);

        service = new GetUnconfirmedAppointmentsService(
                appointmentRepository,
                resultMapper,
                clockPort
        );
    }

    @Test
    void returnsUnconfirmedAppointmentsOrderedByStartTimeAsc_TC04() {
        LocalDate date = LocalDate.of(2026, 9, 14);
        Pageable pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.ASC, "startTime"));

        Appointment apt1 = Appointment.restore(
                UUID.randomUUID(), "APT-001", UUID.randomUUID(), UUID.randomUUID(),
                Instant.parse("2026-09-14T08:30:00Z"), Instant.parse("2026-09-14T09:00:00Z"),
                AppointmentStatus.SCHEDULED, "Khám mắt",
                null, null, null, UUID.randomUUID(), Instant.parse("2026-09-13T08:00:00Z")
        );
        Appointment apt2 = Appointment.restore(
                UUID.randomUUID(), "APT-002", UUID.randomUUID(), UUID.randomUUID(),
                Instant.parse("2026-09-14T09:30:00Z"), Instant.parse("2026-09-14T10:00:00Z"),
                AppointmentStatus.SCHEDULED, "Khám răng",
                null, null, null, UUID.randomUUID(), Instant.parse("2026-09-13T08:00:00Z")
        );

        Page<Appointment> page = new PageImpl<>(List.of(apt1, apt2), pageable, 2);
        when(appointmentRepository.findUnconfirmed(any(Instant.class), any(Instant.class), eq(pageable)))
                .thenReturn(page);

        Page<AppointmentResult> results = service.getUnconfirmed(date, pageable);

        assertNotNull(results);
        assertEquals(2, results.getTotalElements());
        assertEquals("APT-001", results.getContent().get(0).appointmentCode());
        assertEquals("APT-002", results.getContent().get(1).appointmentCode());
        assertEquals(AppointmentStatus.SCHEDULED, results.getContent().get(0).status());
        assertEquals(AppointmentStatus.SCHEDULED, results.getContent().get(1).status());

        verify(appointmentRepository).findUnconfirmed(eq(now), any(Instant.class), eq(pageable));
    }

    @Test
    void returnsUnconfirmedAppointmentsForTodayWhenDateIsNull_Finding4() {
        Pageable pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.ASC, "startTime"));

        Appointment apt1 = Appointment.restore(
                UUID.randomUUID(), "APT-003", UUID.randomUUID(), UUID.randomUUID(),
                Instant.parse("2026-09-14T08:30:00Z"), Instant.parse("2026-09-14T09:00:00Z"),
                AppointmentStatus.SCHEDULED, "Khám tổng quát",
                null, null, null, UUID.randomUUID(), Instant.parse("2026-09-13T08:00:00Z")
        );

        Page<Appointment> page = new PageImpl<>(List.of(apt1), pageable, 1);
        when(appointmentRepository.findUnconfirmed(eq(now), any(Instant.class), eq(pageable)))
                .thenReturn(page);

        Page<AppointmentResult> results = service.getUnconfirmed(null, pageable);

        assertNotNull(results);
        assertEquals(1, results.getTotalElements());
        assertEquals("APT-003", results.getContent().get(0).appointmentCode());
        verify(appointmentRepository).findUnconfirmed(eq(now), any(Instant.class), eq(pageable));
    }

    @Test
    void returnsEmptyPageWhenDateIsInThePast() {
        LocalDate pastDate = LocalDate.of(2026, 9, 13);
        Pageable pageable = PageRequest.of(0, 20);

        Page<AppointmentResult> results = service.getUnconfirmed(pastDate, pageable);

        assertNotNull(results);
        assertEquals(0, results.getTotalElements());
        org.mockito.Mockito.verifyNoInteractions(appointmentRepository);
    }

    @Test
    void queriesFullDayWhenDateIsInTheFuture() {
        LocalDate futureDate = LocalDate.of(2026, 9, 15);
        Pageable pageable = PageRequest.of(0, 20);

        when(appointmentRepository.findUnconfirmed(any(Instant.class), any(Instant.class), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(), pageable, 0));

        Page<AppointmentResult> results = service.getUnconfirmed(futureDate, pageable);

        assertNotNull(results);
        verify(appointmentRepository).findUnconfirmed(any(Instant.class), any(Instant.class), eq(pageable));
    }
}

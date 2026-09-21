package com.benhsoan.application.ucservice.reporting;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

import com.benhsoan.domain.appointment.enums.AppointmentStatus;
import com.benhsoan.domain.shared.exception.ValidationException;
import com.benhsoan.port.dto.result.AppointmentEffectivenessReportResult;
import com.benhsoan.port.outbound.repository.reporting.AppointmentEffectivenessQueryRepository;
import com.benhsoan.port.outbound.repository.reporting.AppointmentStatusCountSummary;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.benhsoan.port.outbound.time.ClockPort;

class GetAppointmentEffectivenessReportServiceTest {

    private static final Instant NOW = Instant.parse("2026-08-31T08:00:00Z");

    private final AppointmentEffectivenessQueryRepository queryRepository =
            mock(AppointmentEffectivenessQueryRepository.class);
    private final ClockPort clockPort = mock(ClockPort.class);
    private final CurrentUserPort currentUserPort = mock(CurrentUserPort.class);

    private final GetAppointmentEffectivenessReportService service =
            new GetAppointmentEffectivenessReportService(queryRepository, clockPort, currentUserPort);

    @Test
    void computesTotalAndPercentagesPerStatus() {
        authorizeManager();
        when(clockPort.now()).thenReturn(NOW);
        when(queryRepository.findStatusCounts(any(), any(), any(), any())).thenReturn(List.of(
                new AppointmentStatusCountSummary("RECEPTION_COUNTER", AppointmentStatus.COMPLETED, 40),
                new AppointmentStatusCountSummary("RECEPTION_COUNTER", AppointmentStatus.CANCELLED, 30),
                new AppointmentStatusCountSummary("RECEPTION_COUNTER", AppointmentStatus.NO_SHOW, 30)
        ));

        AppointmentEffectivenessReportResult result = service.getReport(
                LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31), null, null);

        assertEquals(100, result.total());
        assertEquals(3, result.items().size());
        assertEquals(AppointmentStatus.COMPLETED, result.items().get(0).status());
        assertEquals(40, result.items().get(0).count());
        assertEquals(new BigDecimal("40.00"), result.items().get(0).percentage());
        assertEquals(new BigDecimal("30.00"), result.items().get(1).percentage());
        assertEquals(new BigDecimal("30.00"), result.items().get(2).percentage());
        assertEquals(NOW, result.generatedAt());
    }

    @Test
    void separatesBookingChannelsInSingleResponse() {
        authorizeManager();
        when(clockPort.now()).thenReturn(NOW);
        when(queryRepository.findStatusCounts(any(), any(), any(), any())).thenReturn(List.of(
                new AppointmentStatusCountSummary("ONLINE_PORTAL", AppointmentStatus.COMPLETED, 10),
                new AppointmentStatusCountSummary("RECEPTION_COUNTER", AppointmentStatus.COMPLETED, 30),
                new AppointmentStatusCountSummary("RECEPTION_COUNTER", AppointmentStatus.NO_SHOW, 20)
        ));

        AppointmentEffectivenessReportResult result = service.getReport(
                LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31), null, null);

        assertEquals(60, result.total());
        assertEquals(3, result.items().size());

        var portal = result.items().stream()
                .filter(i -> "ONLINE_PORTAL".equals(i.bookingChannel()))
                .toList();
        var counter = result.items().stream()
                .filter(i -> "RECEPTION_COUNTER".equals(i.bookingChannel()))
                .toList();

        assertEquals(1, portal.size());
        assertEquals(AppointmentStatus.COMPLETED, portal.get(0).status());
        assertEquals(10, portal.get(0).count());
        assertEquals(new BigDecimal("16.67"), portal.get(0).percentage());

        assertEquals(2, counter.size());
        assertEquals(30, counter.get(0).count());
        assertEquals(20, counter.get(1).count());
        assertEquals(new BigDecimal("50.00"), counter.get(0).percentage());
        assertEquals(new BigDecimal("33.33"), counter.get(1).percentage());
    }
    @Test
    void roundsPercentageToTwoDecimalPlacesHalfUp() {
        authorizeManager();
        when(clockPort.now()).thenReturn(NOW);
        when(queryRepository.findStatusCounts(any(), any(), any(), any())).thenReturn(List.of(
                new AppointmentStatusCountSummary("RECEPTION_COUNTER", AppointmentStatus.COMPLETED, 1),
                new AppointmentStatusCountSummary("RECEPTION_COUNTER", AppointmentStatus.CANCELLED, 2)
        ));

        AppointmentEffectivenessReportResult result = service.getReport(
                LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31), null, null);

        assertEquals(3, result.total());
        assertEquals(new BigDecimal("33.33"), result.items().get(0).percentage());
        assertEquals(new BigDecimal("66.67"), result.items().get(1).percentage());
    }

    @Test
    void returnsEmptyReportWhenNoAppointments() {
        authorizeManager();
        when(clockPort.now()).thenReturn(NOW);
        when(queryRepository.findStatusCounts(any(), any(), any(), any())).thenReturn(List.of());

        AppointmentEffectivenessReportResult result = service.getReport(
                LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31), null, null);

        assertEquals(0, result.total());
        assertTrue(result.items().isEmpty());
    }

    @Test
    void ordersItemsByChannelThenStatusDeclarationOrder() {
        authorizeManager();
        when(clockPort.now()).thenReturn(NOW);
        when(queryRepository.findStatusCounts(any(), any(), any(), any())).thenReturn(List.of(
                new AppointmentStatusCountSummary("RECEPTION_COUNTER", AppointmentStatus.NO_SHOW, 5),
                new AppointmentStatusCountSummary("ONLINE_PORTAL", AppointmentStatus.SCHEDULED, 1),
                new AppointmentStatusCountSummary("RECEPTION_COUNTER", AppointmentStatus.SCHEDULED, 2)
        ));

        AppointmentEffectivenessReportResult result = service.getReport(
                LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31), null, null);

        assertEquals(
                List.of("ONLINE_PORTAL", "RECEPTION_COUNTER", "RECEPTION_COUNTER"),
                result.items().stream().map(item -> item.bookingChannel()).toList()
        );
        assertEquals(
                List.of(AppointmentStatus.SCHEDULED, AppointmentStatus.SCHEDULED, AppointmentStatus.NO_SHOW),
                result.items().stream().map(item -> item.status()).toList()
        );
    }

    @Test
    void convertsPeriodBoundariesUsingClinicTimezone() {
        authorizeManager();
        when(queryRepository.findStatusCounts(any(), any(), any(), any())).thenReturn(List.of());

        service.getReport(LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31), null, null);

        verify(queryRepository).findStatusCounts(
                eq(Instant.parse("2026-07-31T17:00:00Z")),
                eq(Instant.parse("2026-08-31T17:00:00Z")),
                eq(null),
                eq(null)
        );
    }

    @Test
    void passesThroughDoctorAndBookingChannelFilters() {
        authorizeManager();
        UUID doctorId = UUID.randomUUID();
        when(queryRepository.findStatusCounts(any(), any(), any(), any())).thenReturn(List.of());

        service.getReport(
                LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31), doctorId, "ONLINE_PORTAL");

        verify(queryRepository).findStatusCounts(
                any(), any(), eq(doctorId), eq("ONLINE_PORTAL")
        );
    }

    @Test
    void rejectsNonManager() {
        when(currentUserPort.hasRole("MANAGER")).thenReturn(false);

        assertThrows(AccessDeniedException.class,
                () -> service.getReport(LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31), null, null));
    }

    @Test
    void rejectsNullFromDate() {
        authorizeManager();

        assertThrows(ValidationException.class,
                () -> service.getReport(null, LocalDate.of(2026, 8, 31), null, null));
    }

    @Test
    void rejectsNullToDate() {
        authorizeManager();

        assertThrows(ValidationException.class,
                () -> service.getReport(LocalDate.of(2026, 8, 1), null, null, null));
    }

    @Test
    void rejectsFromAfterTo() {
        authorizeManager();

        assertThrows(ValidationException.class,
                () -> service.getReport(LocalDate.of(2026, 8, 31), LocalDate.of(2026, 8, 1), null, null));
    }

    @Test
    void acceptsEqualFromAndTo() {
        authorizeManager();
        when(clockPort.now()).thenReturn(NOW);
        when(queryRepository.findStatusCounts(any(), any(), any(), any())).thenReturn(List.of());

        AppointmentEffectivenessReportResult result = service.getReport(
                LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 1), null, null);

        assertEquals(0, result.total());
    }

    @Test
    void acceptsValidDateRange() {
        authorizeManager();
        when(clockPort.now()).thenReturn(NOW);
        when(queryRepository.findStatusCounts(any(), any(), any(), any())).thenReturn(List.of());

        AppointmentEffectivenessReportResult result = service.getReport(
                LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31), null, null);

        assertEquals(0, result.total());
    }

    @Test
    void acceptsNullBookingChannelAsNoFilter() {
        authorizeManager();
        when(clockPort.now()).thenReturn(NOW);
        when(queryRepository.findStatusCounts(any(), any(), any(), any())).thenReturn(List.of());

        service.getReport(LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31), null, null);

        verify(queryRepository).findStatusCounts(any(), any(), eq(null), eq(null));
    }

    @Test
    void acceptsOnlinePortalChannel() {
        authorizeManager();
        when(clockPort.now()).thenReturn(NOW);
        when(queryRepository.findStatusCounts(any(), any(), any(), any())).thenReturn(List.of());

        service.getReport(LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31), null, "ONLINE_PORTAL");

        verify(queryRepository).findStatusCounts(any(), any(), eq(null), eq("ONLINE_PORTAL"));
    }

    @Test
    void acceptsReceptionCounterChannel() {
        authorizeManager();
        when(clockPort.now()).thenReturn(NOW);
        when(queryRepository.findStatusCounts(any(), any(), any(), any())).thenReturn(List.of());

        service.getReport(LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31), null, "RECEPTION_COUNTER");

        verify(queryRepository).findStatusCounts(any(), any(), eq(null), eq("RECEPTION_COUNTER"));
    }

    @Test
    void rejectsInvalidBookingChannel() {
        authorizeManager();

        assertThrows(ValidationException.class,
                () -> service.getReport(LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31), null, "INVALID"));
    }

    @Test
    void rejectsCounterBookingChannel() {
        authorizeManager();

        assertThrows(ValidationException.class,
                () -> service.getReport(LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31), null, "COUNTER"));
    }

    @Test
    void rejectsWebBookingChannel() {
        authorizeManager();

        assertThrows(ValidationException.class,
                () -> service.getReport(LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31), null, "WEB"));
    }

    private void authorizeManager() {
        when(currentUserPort.hasRole("MANAGER")).thenReturn(true);
    }
}

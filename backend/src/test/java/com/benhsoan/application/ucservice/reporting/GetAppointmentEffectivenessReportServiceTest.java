package com.benhsoan.application.ucservice.reporting;

import static org.junit.jupiter.api.Assertions.assertEquals;
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

import com.benhsoan.domain.appointment.enums.AppointmentStatus;
import com.benhsoan.port.dto.result.AppointmentEffectivenessReportResult;
import com.benhsoan.port.outbound.repository.reporting.AppointmentEffectivenessQueryRepository;
import com.benhsoan.port.outbound.repository.reporting.AppointmentStatusCountSummary;
import com.benhsoan.port.outbound.time.ClockPort;

class GetAppointmentEffectivenessReportServiceTest {

    private static final Instant NOW = Instant.parse("2026-08-31T08:00:00Z");

    private final AppointmentEffectivenessQueryRepository queryRepository =
            mock(AppointmentEffectivenessQueryRepository.class);
    private final ClockPort clockPort = mock(ClockPort.class);

    private final GetAppointmentEffectivenessReportService service =
            new GetAppointmentEffectivenessReportService(queryRepository, clockPort);

    @Test
    void computesTotalAndPercentagesPerStatus() {
        when(clockPort.now()).thenReturn(NOW);
        when(queryRepository.findStatusCounts(any(), any(), any(), any())).thenReturn(List.of(
                new AppointmentStatusCountSummary(AppointmentStatus.COMPLETED, 40),
                new AppointmentStatusCountSummary(AppointmentStatus.CANCELLED, 30),
                new AppointmentStatusCountSummary(AppointmentStatus.NO_SHOW, 30)
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
    void roundsPercentageToTwoDecimalPlacesHalfUp() {
        when(clockPort.now()).thenReturn(NOW);
        when(queryRepository.findStatusCounts(any(), any(), any(), any())).thenReturn(List.of(
                new AppointmentStatusCountSummary(AppointmentStatus.COMPLETED, 1),
                new AppointmentStatusCountSummary(AppointmentStatus.CANCELLED, 2)
        ));

        AppointmentEffectivenessReportResult result = service.getReport(
                LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31), null, null);

        assertEquals(3, result.total());
        assertEquals(new BigDecimal("33.33"), result.items().get(0).percentage());
        assertEquals(new BigDecimal("66.67"), result.items().get(1).percentage());
    }

    @Test
    void returnsEmptyReportWhenNoAppointments() {
        when(clockPort.now()).thenReturn(NOW);
        when(queryRepository.findStatusCounts(any(), any(), any(), any())).thenReturn(List.of());

        AppointmentEffectivenessReportResult result = service.getReport(
                LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31), null, null);

        assertEquals(0, result.total());
        assertTrue(result.items().isEmpty());
    }

    @Test
    void ordersItemsByStatusDeclarationOrder() {
        when(clockPort.now()).thenReturn(NOW);
        when(queryRepository.findStatusCounts(any(), any(), any(), any())).thenReturn(List.of(
                new AppointmentStatusCountSummary(AppointmentStatus.NO_SHOW, 5),
                new AppointmentStatusCountSummary(AppointmentStatus.SCHEDULED, 1),
                new AppointmentStatusCountSummary(AppointmentStatus.COMPLETED, 10)
        ));

        AppointmentEffectivenessReportResult result = service.getReport(
                LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31), null, null);

        assertEquals(
                List.of(AppointmentStatus.SCHEDULED, AppointmentStatus.COMPLETED, AppointmentStatus.NO_SHOW),
                result.items().stream().map(item -> item.status()).toList()
        );
    }

    @Test
    void convertsPeriodBoundariesUsingClinicTimezone() {
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
        UUID doctorId = UUID.randomUUID();
        when(queryRepository.findStatusCounts(any(), any(), any(), any())).thenReturn(List.of());

        service.getReport(
                LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31), doctorId, "ONLINE_PORTAL");

        verify(queryRepository).findStatusCounts(
                any(), any(), eq(doctorId), eq("ONLINE_PORTAL")
        );
    }
}

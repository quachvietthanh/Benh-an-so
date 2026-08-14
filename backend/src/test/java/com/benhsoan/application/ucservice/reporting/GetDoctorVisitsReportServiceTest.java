package com.benhsoan.application.ucservice.reporting;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

import com.benhsoan.port.dto.result.DoctorVisitsReportResult;
import com.benhsoan.port.dto.result.DoctorVisitSummaryResult;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.benhsoan.port.outbound.time.ClockPort;

class GetDoctorVisitsReportServiceTest {

    private final OperationalReportDataService operationalReportDataService =
            mock(OperationalReportDataService.class);
    private final CurrentUserPort currentUserPort = mock(CurrentUserPort.class);
    private final ClockPort clockPort = mock(ClockPort.class);

    private final GetDoctorVisitsReportService service =
            new GetDoctorVisitsReportService(operationalReportDataService, currentUserPort, clockPort);

    @Test
    void delegatesAndStampsGeneratedAtWhenAuthorized() {
        when(currentUserPort.hasRole("MANAGER")).thenReturn(true);
        Instant generatedAt = Instant.parse("2026-08-14T08:00:00Z");
        when(clockPort.now()).thenReturn(generatedAt);
        when(operationalReportDataService.getDoctorVisits(any(), any()))
                .thenReturn(new DoctorVisitsReportResult(
                        LocalDate.of(2026, 8, 1),
                        LocalDate.of(2026, 8, 14),
                        null,
                        List.of(new DoctorVisitSummaryResult(
                                1,
                                UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa2"),
                                "doctor1",
                                "Dr. Nguyen Minh Anh",
                                25L))
                ));

        DoctorVisitsReportResult result =
                service.getDoctorVisits(LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 14));

        assertEquals(generatedAt, result.generatedAt());
        assertEquals(1, result.items().size());
        assertEquals(1, result.items().get(0).rank());
        assertEquals(25L, result.items().get(0).totalVisits());
        verify(operationalReportDataService)
                .getDoctorVisits(LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 14));
    }

    @Test
    void rejectsNonManager() {
        when(currentUserPort.hasRole("MANAGER")).thenReturn(false);

        assertThrows(AccessDeniedException.class,
                () -> service.getDoctorVisits(LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 14)));
    }
}

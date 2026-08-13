package com.benhsoan.application.ucservice.reporting;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.benhsoan.port.outbound.repository.reporting.DailyRevenueSummary;
import com.benhsoan.port.outbound.repository.reporting.DailyVisitSummary;
import com.benhsoan.port.outbound.repository.reporting.OperationalReportQueryRepository;

class GetOperationalTimelineServiceTest {

    @Test
    void returnsTimelineFromSharedReportingDataService() {
        OperationalReportDataService dataService = mock(OperationalReportDataService.class);
        when(dataService.getTimeline(LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 3)))
                .thenReturn(new com.benhsoan.port.dto.result.OperationalTimelineResult(
                        LocalDate.of(2026, 8, 1),
                        LocalDate.of(2026, 8, 3),
                        List.of(
                                new com.benhsoan.port.dto.result.OperationalTimelineItemResult(
                                        LocalDate.of(2026, 8, 1), 2L, new BigDecimal("100000"))
                        )
                ));

        var result = new GetOperationalTimelineService(dataService)
                .getTimeline(LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 3));

        assertEquals(LocalDate.of(2026, 8, 1), result.from());
        assertEquals(LocalDate.of(2026, 8, 3), result.to());
        assertEquals(1, result.items().size());
        assertEquals(2L, result.items().getFirst().visitCount());
    }

    @Test
    void returnsTimelineForEveryDayIncludingEmptyDaysAndNegativeRevenueDays() {
        OperationalReportQueryRepository repository = mock(OperationalReportQueryRepository.class);
        Instant fromInclusive = Instant.parse("2026-07-31T17:00:00Z");
        Instant toExclusive = Instant.parse("2026-08-03T17:00:00Z");

        when(repository.findDailyCompletedVisits(fromInclusive, toExclusive)).thenReturn(List.of(
                new DailyVisitSummary(LocalDate.of(2026, 8, 1), 2L),
                new DailyVisitSummary(LocalDate.of(2026, 8, 3), 1L)
        ));
        when(repository.findDailyNetRevenue(fromInclusive, toExclusive)).thenReturn(List.of(
                new DailyRevenueSummary(LocalDate.of(2026, 8, 1), new BigDecimal("100000")),
                new DailyRevenueSummary(LocalDate.of(2026, 8, 3), new BigDecimal("-20000"))
        ));

        var result = new GetOperationalTimelineService(new OperationalReportDataService(repository))
                .getTimeline(LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 3));

        assertEquals(3, result.items().size());
        assertEquals(LocalDate.of(2026, 8, 2), result.items().get(1).date());
        assertEquals(0L, result.items().get(1).visitCount());
        assertEquals(BigDecimal.ZERO, result.items().get(1).revenue());
        assertEquals(new BigDecimal("-20000"), result.items().get(2).revenue());
    }
}

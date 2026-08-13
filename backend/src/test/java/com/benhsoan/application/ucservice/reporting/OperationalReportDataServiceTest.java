package com.benhsoan.application.ucservice.reporting;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
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

class OperationalReportDataServiceTest {

    @Test
    void buildsSummaryAndTimelineFromSharedReportingDataSource() {
        OperationalReportQueryRepository repository = mock(OperationalReportQueryRepository.class);
        when(repository.countCompletedVisits(any(), any())).thenReturn(3L);
        when(repository.sumNetRevenue(any(), any())).thenReturn(new BigDecimal("80000"));
        when(repository.findDailyCompletedVisits(any(), any())).thenReturn(List.of(
                new DailyVisitSummary(LocalDate.of(2026, 8, 1), 2L),
                new DailyVisitSummary(LocalDate.of(2026, 8, 3), 1L)
        ));
        when(repository.findDailyNetRevenue(any(), any())).thenReturn(List.of(
                new DailyRevenueSummary(LocalDate.of(2026, 8, 1), new BigDecimal("100000")),
                new DailyRevenueSummary(LocalDate.of(2026, 8, 3), new BigDecimal("-20000"))
        ));

        OperationalReportDataService service = new OperationalReportDataService(repository);
        OperationalReportData reportData = service.getReportData(
                LocalDate.of(2026, 8, 1),
                LocalDate.of(2026, 8, 3)
        );

        assertEquals(3L, reportData.summary().visitCount());
        assertEquals(new BigDecimal("80000"), reportData.summary().revenue());
        assertEquals("VND", reportData.summary().currency());
        assertEquals(3, reportData.timeline().items().size());
        assertEquals(0L, reportData.timeline().items().get(1).visitCount());
        assertEquals(BigDecimal.ZERO, reportData.timeline().items().get(1).revenue());
        assertEquals(new BigDecimal("-20000"), reportData.timeline().items().get(2).revenue());
    }
}

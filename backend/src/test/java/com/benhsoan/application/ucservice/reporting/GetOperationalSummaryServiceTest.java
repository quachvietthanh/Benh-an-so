package com.benhsoan.application.ucservice.reporting;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import com.benhsoan.port.dto.result.OperationalSummaryResult;

class GetOperationalSummaryServiceTest {

    @Test
    void returnsSummaryFromSharedReportingDataService() {
        OperationalReportDataService dataService = mock(OperationalReportDataService.class);
        when(dataService.getSummary(LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 3)))
                .thenReturn(new OperationalSummaryResult(
                        LocalDate.of(2026, 8, 1),
                        LocalDate.of(2026, 8, 3),
                        12L,
                        new BigDecimal("5400000"),
                        "VND"
                ));

        OperationalSummaryResult result = new GetOperationalSummaryService(dataService)
                .getSummary(LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 3));

        assertEquals(LocalDate.of(2026, 8, 1), result.from());
        assertEquals(LocalDate.of(2026, 8, 3), result.to());
        assertEquals(12L, result.visitCount());
        assertEquals(new BigDecimal("5400000"), result.revenue());
        assertEquals("VND", result.currency());
    }
}

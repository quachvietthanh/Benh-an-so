package com.benhsoan.application.ucservice.reporting;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.benhsoan.port.dto.result.OperationalReportExportResult;
import com.benhsoan.port.dto.result.OperationalSummaryResult;
import com.benhsoan.port.dto.result.OperationalTimelineItemResult;
import com.benhsoan.port.dto.result.OperationalTimelineResult;

class ExportOperationalReportServiceTest {

    @Test
    void exportsCsvAndWritesAuditLog() {
        OperationalReportDataService dataService = mock(OperationalReportDataService.class);
        OperationalReportAuditService auditService = mock(OperationalReportAuditService.class);

        when(dataService.getReportData(any(), any())).thenReturn(new OperationalReportData(
                new OperationalSummaryResult(
                        LocalDate.of(2026, 8, 1),
                        LocalDate.of(2026, 8, 3),
                        3L,
                        new BigDecimal("80000"),
                        "VND"
                ),
                new OperationalTimelineResult(
                        LocalDate.of(2026, 8, 1),
                        LocalDate.of(2026, 8, 3),
                        List.of(
                                new OperationalTimelineItemResult(LocalDate.of(2026, 8, 1), 2L, new BigDecimal("100000")),
                                new OperationalTimelineItemResult(LocalDate.of(2026, 8, 2), 0L, BigDecimal.ZERO),
                                new OperationalTimelineItemResult(LocalDate.of(2026, 8, 3), 1L, new BigDecimal("-20000"))
                        )
                )
        ));

        ExportOperationalReportService service = new ExportOperationalReportService(
                dataService,
                auditService
        );

        OperationalReportExportResult result = service.export(
                LocalDate.of(2026, 8, 1),
                LocalDate.of(2026, 8, 3)
        );

        assertEquals("operational-report-2026-08-01-to-2026-08-03.csv", result.fileName());
        assertEquals("text/csv; charset=UTF-8", result.contentType());
        assertArrayEquals("""
                \uFEFFOPERATIONAL REPORT
                From,2026-08-01
                To,2026-08-03
                Visit Count,3
                Revenue (VND),80000

                Date,Visit Count,Revenue (VND)
                2026-08-01,2,100000
                2026-08-02,0,0
                2026-08-03,1,-20000
                """.getBytes(StandardCharsets.UTF_8), result.content());
        verify(auditService).logExport(LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 3));
    }
}

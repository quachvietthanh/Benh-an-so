package com.benhsoan.application.ucservice.reporting;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.benhsoan.domain.reporting.enums.ReportType;
import com.benhsoan.domain.reporting.exception.OperationalReportDataEmptyException;
import com.benhsoan.port.dto.result.OperationalReportExportResult;
import com.benhsoan.port.dto.result.OperationalSummaryResult;
import com.benhsoan.port.dto.result.OperationalTimelineItemResult;
import com.benhsoan.port.dto.result.OperationalTimelineResult;

class ExportOperationalReportServiceTest {

    @Test
    void exportsCsvAndWritesAuditLog() {
        OperationalReportDataService dataService = mock(OperationalReportDataService.class);
        OperationalReportAuditService auditService = mock(OperationalReportAuditService.class);
        when(dataService.hasReportData(any(), any(), any())).thenReturn(true);

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
                ReportType.OPERATIONAL_REPORT,
                LocalDate.of(2026, 8, 1),
                LocalDate.of(2026, 8, 3)
        );

        assertEquals(ReportType.OPERATIONAL_REPORT, result.reportType());
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
        verify(auditService).logExport(ReportType.OPERATIONAL_REPORT, LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 3));
    }

    @Test
    void exportsVisitCsvWithVisitDataOnly() {
        OperationalReportDataService dataService = mock(OperationalReportDataService.class);
        OperationalReportAuditService auditService = mock(OperationalReportAuditService.class);
        when(dataService.hasReportData(any(), any(), any())).thenReturn(true);
        when(dataService.getReportData(any(), any())).thenReturn(sampleReportData());

        OperationalReportExportResult result = new ExportOperationalReportService(dataService, auditService).export(
                ReportType.VISIT_REPORT, LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 3));

        assertEquals("visit-report-2026-08-01-to-2026-08-03.csv", result.fileName());
        assertArrayEquals("""
                \uFEFFVISIT REPORT
                From,2026-08-01
                To,2026-08-03
                Visit Count,3

                Date,Visit Count
                2026-08-01,2
                2026-08-02,0
                2026-08-03,1
                """.getBytes(StandardCharsets.UTF_8), result.content());
        verify(auditService).logExport(ReportType.VISIT_REPORT, LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 3));
    }

    @Test
    void exportsRevenueCsvWithRevenueDataOnly() {
        OperationalReportDataService dataService = mock(OperationalReportDataService.class);
        OperationalReportAuditService auditService = mock(OperationalReportAuditService.class);
        when(dataService.hasReportData(any(), any(), any())).thenReturn(true);
        when(dataService.getReportData(any(), any())).thenReturn(sampleReportData());

        OperationalReportExportResult result = new ExportOperationalReportService(dataService, auditService).export(
                ReportType.REVENUE_REPORT, LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 3));

        assertEquals("revenue-report-2026-08-01-to-2026-08-03.csv", result.fileName());
        assertArrayEquals("""
                \uFEFFREVENUE REPORT
                From,2026-08-01
                To,2026-08-03
                Revenue (VND),80000

                Date,Revenue (VND)
                2026-08-01,100000
                2026-08-02,0
                2026-08-03,-20000
                """.getBytes(StandardCharsets.UTF_8), result.content());
        verify(auditService).logExport(ReportType.REVENUE_REPORT, LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 3));
    }

    @Test
    void rejectsExportWithoutOperationalDataAndDoesNotWriteAuditLog() {
        OperationalReportDataService dataService = mock(OperationalReportDataService.class);
        OperationalReportAuditService auditService = mock(OperationalReportAuditService.class);
        when(dataService.hasReportData(any(), any(), any())).thenReturn(false);

        ExportOperationalReportService service = new ExportOperationalReportService(dataService, auditService);

        OperationalReportDataEmptyException exception = assertThrows(
                OperationalReportDataEmptyException.class,
                () -> service.export(ReportType.OPERATIONAL_REPORT, LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 3))
        );

        assertEquals("No report data available for the selected period.", exception.getMessage());
        verify(dataService, never()).getReportData(any(), any());
        verifyNoInteractions(auditService);
    }

    @Test
    void exportsRevenueReportWhenInvoicesExistEvenIfNetRevenueIsZero() {
        OperationalReportDataService dataService = mock(OperationalReportDataService.class);
        OperationalReportAuditService auditService = mock(OperationalReportAuditService.class);
        when(dataService.hasReportData(any(), any(), any())).thenReturn(true);
        when(dataService.getReportData(any(), any())).thenReturn(new OperationalReportData(
                new OperationalSummaryResult(
                        LocalDate.of(2026, 8, 1),
                        LocalDate.of(2026, 8, 2),
                        0L,
                        BigDecimal.ZERO,
                        "VND"
                ),
                new OperationalTimelineResult(
                        LocalDate.of(2026, 8, 1),
                        LocalDate.of(2026, 8, 2),
                        List.of(
                                new OperationalTimelineItemResult(
                                        LocalDate.of(2026, 8, 1), 0L, new BigDecimal("100000")),
                                new OperationalTimelineItemResult(
                                        LocalDate.of(2026, 8, 2), 0L, new BigDecimal("-100000"))
                        )
                )
        ));

        OperationalReportExportResult result = new ExportOperationalReportService(dataService, auditService).export(
                ReportType.REVENUE_REPORT, LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 2));

        assertEquals(ReportType.REVENUE_REPORT, result.reportType());
        assertEquals("revenue-report-2026-08-01-to-2026-08-02.csv", result.fileName());
        verify(auditService).logExport(ReportType.REVENUE_REPORT, LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 2));
    }

    private OperationalReportData sampleReportData() {
        return new OperationalReportData(
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
        );
    }
}

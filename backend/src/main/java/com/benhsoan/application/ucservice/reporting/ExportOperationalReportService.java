package com.benhsoan.application.ucservice.reporting;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.domain.reporting.enums.ReportType;
import com.benhsoan.domain.reporting.exception.OperationalReportDataEmptyException;
import com.benhsoan.port.dto.result.OperationalReportExportResult;
import com.benhsoan.port.dto.result.OperationalSummaryResult;
import com.benhsoan.port.dto.result.OperationalTimelineItemResult;
import com.benhsoan.port.dto.result.OperationalTimelineResult;
import com.benhsoan.port.inbound.reporting.ExportOperationalReportUseCase;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class ExportOperationalReportService implements ExportOperationalReportUseCase {

    private static final String CSV_CONTENT_TYPE = "text/csv; charset=UTF-8";

    private final OperationalReportDataService operationalReportDataService;
    private final OperationalReportAuditService operationalReportAuditService;

    @Override
    public OperationalReportExportResult export(ReportType reportType, LocalDate from, LocalDate to) {
        if (!operationalReportDataService.hasReportData(reportType, from, to)) {
            throw new OperationalReportDataEmptyException();
        }

        OperationalReportData reportData = operationalReportDataService.getReportData(from, to);
        OperationalSummaryResult summary = reportData.summary();
        OperationalTimelineResult timeline = reportData.timeline();
        String fileName = buildFileName(reportType, from, to);
        byte[] content = buildCsv(reportType, summary, timeline).getBytes(StandardCharsets.UTF_8);

        operationalReportAuditService.logExport(reportType, from, to);

        return new OperationalReportExportResult(
                reportType,
                fileName,
                CSV_CONTENT_TYPE,
                content
        );
    }

    private String buildFileName(ReportType reportType, LocalDate from, LocalDate to) {
        String prefix = switch (reportType) {
            case VISIT_REPORT -> "visit-report";
            case REVENUE_REPORT -> "revenue-report";
            case OPERATIONAL_REPORT -> "operational-report";
        };
        return prefix + "-" + from + "-to-" + to + ".csv";
    }

    private String buildCsv(
            ReportType reportType,
            OperationalSummaryResult summary,
            OperationalTimelineResult timeline
    ) {
        return switch (reportType) {
            case VISIT_REPORT -> buildVisitCsv(summary, timeline);
            case REVENUE_REPORT -> buildRevenueCsv(summary, timeline);
            case OPERATIONAL_REPORT -> buildOperationalCsv(summary, timeline);
        };
    }

    private String buildVisitCsv(OperationalSummaryResult summary, OperationalTimelineResult timeline) {
        StringBuilder csv = new StringBuilder();
        csv.append('\uFEFF');
        csv.append("VISIT REPORT\n");
        csv.append("From,").append(summary.from()).append('\n');
        csv.append("To,").append(summary.to()).append('\n');
        csv.append("Visit Count,").append(summary.visitCount()).append("\n\n");
        csv.append("Date,Visit Count\n");

        for (OperationalTimelineItemResult item : timeline.items()) {
            csv.append(item.date()).append(',').append(item.visitCount()).append('\n');
        }

        return csv.toString();
    }

    private String buildRevenueCsv(OperationalSummaryResult summary, OperationalTimelineResult timeline) {
        StringBuilder csv = new StringBuilder();
        csv.append('\uFEFF');
        csv.append("REVENUE REPORT\n");
        csv.append("From,").append(summary.from()).append('\n');
        csv.append("To,").append(summary.to()).append('\n');
        csv.append("Revenue (").append(summary.currency()).append("),").append(summary.revenue()).append("\n\n");
        csv.append("Date,Revenue (").append(summary.currency()).append(")\n");

        for (OperationalTimelineItemResult item : timeline.items()) {
            csv.append(item.date()).append(',').append(item.revenue()).append('\n');
        }

        return csv.toString();
    }

    private String buildOperationalCsv(OperationalSummaryResult summary, OperationalTimelineResult timeline) {
        StringBuilder csv = new StringBuilder();
        csv.append('\uFEFF');
        csv.append("OPERATIONAL REPORT\n");
        csv.append("From,").append(summary.from()).append('\n');
        csv.append("To,").append(summary.to()).append('\n');
        csv.append("Visit Count,").append(summary.visitCount()).append('\n');
        csv.append("Revenue (").append(summary.currency()).append("),").append(summary.revenue()).append("\n\n");
        csv.append("Date,Visit Count,Revenue (").append(summary.currency()).append(")\n");

        for (OperationalTimelineItemResult item : timeline.items()) {
            csv.append(item.date()).append(',')
                    .append(item.visitCount()).append(',')
                    .append(item.revenue()).append('\n');
        }

        return csv.toString();
    }
}

package com.benhsoan.application.ucservice.reporting;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    public OperationalReportExportResult export(LocalDate from, LocalDate to) {
        OperationalReportData reportData = operationalReportDataService.getReportData(from, to);
        OperationalSummaryResult summary = reportData.summary();
        OperationalTimelineResult timeline = reportData.timeline();
        String fileName = "operational-report-" + from + "-to-" + to + ".csv";
        byte[] content = buildCsv(summary, timeline).getBytes(StandardCharsets.UTF_8);

        operationalReportAuditService.logExport(from, to);

        return new OperationalReportExportResult(
                fileName,
                CSV_CONTENT_TYPE,
                content
        );
    }

    private String buildCsv(OperationalSummaryResult summary, OperationalTimelineResult timeline) {
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

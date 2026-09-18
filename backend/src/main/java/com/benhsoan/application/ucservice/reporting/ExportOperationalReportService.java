package com.benhsoan.application.ucservice.reporting;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Locale;
import java.util.UUID;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.domain.auth.User;
import com.benhsoan.domain.reporting.enums.ReportType;
import com.benhsoan.domain.reporting.exception.OperationalReportDataEmptyException;
import com.benhsoan.domain.shared.exception.ValidationException;
import com.benhsoan.port.dto.result.DiseasePatternItemResult;
import com.benhsoan.port.dto.result.DiseasePatternReportResult;
import com.benhsoan.port.dto.result.OperationalReportExportResult;
import com.benhsoan.port.dto.result.OperationalSummaryResult;
import com.benhsoan.port.dto.result.OperationalTimelineItemResult;
import com.benhsoan.port.dto.result.OperationalTimelineResult;
import com.benhsoan.port.inbound.reporting.ExportOperationalReportUseCase;
import com.benhsoan.port.outbound.repository.auth.UserRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class ExportOperationalReportService implements ExportOperationalReportUseCase {

    private static final String CSV_CONTENT_TYPE = "text/csv; charset=UTF-8";

    private final OperationalReportDataService operationalReportDataService;
    private final OperationalReportAuditService operationalReportAuditService;
    private final UserRepository userRepository;
    private final CurrentUserPort currentUserPort;

    @Override
    public OperationalReportExportResult export(ReportType reportType, LocalDate from, LocalDate to) {
        return export(reportType, from, to, null);
    }

    @Override
    public OperationalReportExportResult export(ReportType reportType, LocalDate from, LocalDate to, UUID doctorId) {
        String doctorName = null;
        if (reportType == ReportType.DISEASE_PATTERN_REPORT) {
            if (!currentUserPort.hasRole("MANAGER")) {
                operationalReportAuditService.logAccessDenied(
                        ReportType.DISEASE_PATTERN_REPORT,
                        "Only managers can export the disease pattern report."
                );
                throw new AccessDeniedException("Only managers can export the disease pattern report.");
            }
            if (doctorId != null) {
                User doctor = userRepository.findById(doctorId)
                        .orElseThrow(() -> new ValidationException("Doctor not found."));
                doctorName = doctor.getFullName();
            }
        }

        boolean hasData = doctorId == null
                ? operationalReportDataService.hasReportData(reportType, from, to)
                : operationalReportDataService.hasReportData(reportType, from, to, doctorId);
        if (!hasData) {
            throw new OperationalReportDataEmptyException();
        }

        String fileName;
        byte[] content;

        if (reportType == ReportType.DISEASE_PATTERN_REPORT) {
            DiseasePatternReportResult reportResult = operationalReportDataService.getDiseasePatterns(from, to, doctorId, doctorName);
            fileName = buildDiseasePatternFileName(from, to, doctorId);
            content = buildDiseasePatternCsv(reportResult).getBytes(StandardCharsets.UTF_8);
        } else {
            OperationalReportData reportData = operationalReportDataService.getReportData(from, to);
            OperationalSummaryResult summary = reportData.summary();
            OperationalTimelineResult timeline = reportData.timeline();
            fileName = buildFileName(reportType, from, to);
            content = buildCsv(reportType, summary, timeline).getBytes(StandardCharsets.UTF_8);
        }

        if (doctorId == null) {
            operationalReportAuditService.logExport(reportType, from, to);
        } else {
            operationalReportAuditService.logExport(reportType, from, to, doctorId);
        }

        return new OperationalReportExportResult(
                reportType,
                fileName,
                CSV_CONTENT_TYPE,
                content
        );
    }

    private String buildDiseasePatternFileName(LocalDate from, LocalDate to, UUID doctorId) {
        if (doctorId != null) {
            return "disease-pattern-report-" + from + "-to-" + to + "-doctor-" + doctorId + ".csv";
        }
        return "disease-pattern-report-" + from + "-to-" + to + ".csv";
    }

    private String buildDiseasePatternCsv(DiseasePatternReportResult report) {
        StringBuilder csv = new StringBuilder();
        csv.append('\uFEFF');
        csv.append("DISEASE PATTERN REPORT\n");
        csv.append("From,").append(report.from()).append('\n');
        csv.append("To,").append(report.to()).append('\n');
        csv.append("Doctor,").append(report.doctorName() != null ? report.doctorName() : "All Doctors").append('\n');
        csv.append("Total Diagnoses,").append(report.totalDiagnoses()).append("\n\n");
        csv.append("Rank,Disease Code,Disease Name,Disease Group,Diagnosis Count,Percentage\n");

        for (DiseasePatternItemResult item : report.items()) {
            csv.append(item.rank()).append(',')
                    .append(escapeCsv(item.diseaseCode())).append(',')
                    .append(escapeCsv(item.diseaseName())).append(',')
                    .append(escapeCsv(item.diseaseGroup())).append(',')
                    .append(item.diagnosisCount()).append(',')
                    .append(String.format(Locale.US, "%.2f%%", item.percentage()))
                    .append('\n');
        }

        return csv.toString();
    }

    private String escapeCsv(String value) {
        if (value == null) {
            return "";
        }
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    private String buildFileName(ReportType reportType, LocalDate from, LocalDate to) {
        String prefix = switch (reportType) {
            case VISIT_REPORT -> "visit-report";
            case REVENUE_REPORT -> "revenue-report";
            case OPERATIONAL_REPORT -> "operational-report";
            case DISEASE_PATTERN_REPORT -> "disease-pattern-report";
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
            case DISEASE_PATTERN_REPORT -> throw new IllegalArgumentException("Use buildDiseasePatternCsv for DISEASE_PATTERN_REPORT");
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

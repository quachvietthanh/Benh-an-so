package com.benhsoan.port.inbound.reporting;

import java.time.LocalDate;

import java.util.UUID;

import com.benhsoan.domain.reporting.enums.ReportType;
import com.benhsoan.port.dto.result.OperationalReportExportResult;

public interface ExportOperationalReportUseCase {

    default OperationalReportExportResult export(ReportType reportType, LocalDate from, LocalDate to) {
        return export(reportType, from, to, null);
    }

    OperationalReportExportResult export(ReportType reportType, LocalDate from, LocalDate to, UUID doctorId);
}

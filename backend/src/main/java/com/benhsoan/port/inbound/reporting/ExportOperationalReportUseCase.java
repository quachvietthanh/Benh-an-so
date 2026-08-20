package com.benhsoan.port.inbound.reporting;

import java.time.LocalDate;

import com.benhsoan.domain.reporting.enums.ReportType;
import com.benhsoan.port.dto.result.OperationalReportExportResult;

public interface ExportOperationalReportUseCase {

    OperationalReportExportResult export(ReportType reportType, LocalDate from, LocalDate to);
}

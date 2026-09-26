package com.benhsoan.port.inbound.reporting;

import java.time.LocalDate;

import com.benhsoan.port.dto.result.AccessLogReportExportResult;

public interface ExportAccessLogReportUseCase {

    AccessLogReportExportResult export(LocalDate from, LocalDate to);
}

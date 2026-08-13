package com.benhsoan.port.inbound.reporting;

import java.time.LocalDate;

import com.benhsoan.port.dto.result.OperationalReportExportResult;

public interface ExportOperationalReportUseCase {

    OperationalReportExportResult export(LocalDate from, LocalDate to);
}

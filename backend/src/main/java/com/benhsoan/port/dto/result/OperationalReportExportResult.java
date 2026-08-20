package com.benhsoan.port.dto.result;

import com.benhsoan.domain.reporting.enums.ReportType;

public record OperationalReportExportResult(
        ReportType reportType,
        String fileName,
        String contentType,
        byte[] content
) {
}

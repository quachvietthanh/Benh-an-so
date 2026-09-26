package com.benhsoan.port.dto.result;

public record AccessLogReportExportResult(
        String fileName,
        String contentType,
        byte[] content
) {
}

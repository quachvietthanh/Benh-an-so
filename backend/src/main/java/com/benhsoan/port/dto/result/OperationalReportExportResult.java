package com.benhsoan.port.dto.result;

public record OperationalReportExportResult(
        String fileName,
        String contentType,
        byte[] content
) {
}

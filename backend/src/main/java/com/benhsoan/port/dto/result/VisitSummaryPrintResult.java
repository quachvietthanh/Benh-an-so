package com.benhsoan.port.dto.result;

public record VisitSummaryPrintResult(
        String fileName,
        String contentType,
        byte[] content
) {
}

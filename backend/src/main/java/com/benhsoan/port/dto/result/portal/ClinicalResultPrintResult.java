package com.benhsoan.port.dto.result.portal;

public record ClinicalResultPrintResult(
        String fileName,
        String contentType,
        byte[] content
) {
}

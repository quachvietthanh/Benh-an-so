package com.benhsoan.port.dto.result.portal;

public record InvoicePrintResult(
        String fileName,
        String contentType,
        byte[] content
) {
}

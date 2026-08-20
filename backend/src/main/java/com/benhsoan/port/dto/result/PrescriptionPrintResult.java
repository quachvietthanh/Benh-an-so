package com.benhsoan.port.dto.result;

public record PrescriptionPrintResult(
        String fileName,
        String contentType,
        byte[] content
) {
}

package com.benhsoan.port.dto.result;

public record MedicalRecordCopyResult(
        String fileName,
        String contentType,
        byte[] content
) {
}

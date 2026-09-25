package com.benhsoan.port.dto.result;

/**
 * Result of exporting medical records according to data exchange structure (NCL-11-CN-007).
 */
public record MedicalRecordExchangeExportResult(
        String fileName,
        String contentType,
        byte[] content,
        int recordCount
) {
}

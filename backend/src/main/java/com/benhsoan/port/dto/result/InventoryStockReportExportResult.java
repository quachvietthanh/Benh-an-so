package com.benhsoan.port.dto.result;

public record InventoryStockReportExportResult(
        String fileName,
        String contentType,
        byte[] content
) {
}

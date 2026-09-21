package com.benhsoan.port.dto.result.inventory;

public record InventoryInOutStockExportResult(
        String fileName,
        String contentType,
        byte[] content
) {
}

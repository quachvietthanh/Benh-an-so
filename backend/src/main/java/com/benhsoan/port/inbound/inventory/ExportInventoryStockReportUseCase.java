package com.benhsoan.port.inbound.inventory;

import java.time.LocalDate;

import com.benhsoan.port.dto.result.InventoryStockReportExportResult;

public interface ExportInventoryStockReportUseCase {

    InventoryStockReportExportResult export(LocalDate from, LocalDate to);
}

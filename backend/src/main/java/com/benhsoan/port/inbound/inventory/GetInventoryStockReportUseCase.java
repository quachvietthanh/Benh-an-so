package com.benhsoan.port.inbound.inventory;

import java.time.LocalDate;

import com.benhsoan.port.dto.result.InventoryStockReportResult;

public interface GetInventoryStockReportUseCase {

    InventoryStockReportResult getStockReport(LocalDate from, LocalDate to);
}

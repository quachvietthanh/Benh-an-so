package com.benhsoan.port.inbound.inventory;

import com.benhsoan.port.dto.query.inventory.GetInventoryInOutStockReportQuery;
import com.benhsoan.port.dto.result.inventory.InventoryInOutStockReportResult;

public interface GetInventoryInOutStockReportUseCase {

    InventoryInOutStockReportResult getReport(GetInventoryInOutStockReportQuery query);
}

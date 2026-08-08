package com.benhsoan.port.inbound.inventory;

import java.util.List;

import com.benhsoan.port.dto.query.inventory.ListInventoryStocksQuery;
import com.benhsoan.port.dto.result.InventoryStockResult;

public interface ListInventoryStocksUseCase {

    List<InventoryStockResult> list(ListInventoryStocksQuery query);
}

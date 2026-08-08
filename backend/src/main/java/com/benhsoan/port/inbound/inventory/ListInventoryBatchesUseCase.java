package com.benhsoan.port.inbound.inventory;

import java.util.List;

import com.benhsoan.port.dto.query.inventory.ListInventoryBatchesQuery;
import com.benhsoan.port.dto.result.InventoryBatchResult;

public interface ListInventoryBatchesUseCase {

    List<InventoryBatchResult> list(ListInventoryBatchesQuery query);
}

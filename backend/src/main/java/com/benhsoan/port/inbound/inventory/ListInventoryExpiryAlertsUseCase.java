package com.benhsoan.port.inbound.inventory;

import java.util.List;

import com.benhsoan.port.dto.query.inventory.ListInventoryExpiryAlertsQuery;
import com.benhsoan.port.dto.result.InventoryExpiryAlertResult;

public interface ListInventoryExpiryAlertsUseCase {

    List<InventoryExpiryAlertResult> list(ListInventoryExpiryAlertsQuery query);
}

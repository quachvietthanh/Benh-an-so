package com.benhsoan.adapter.inbound.rest.request.inventory;

import java.util.Map;
import java.util.UUID;

public record ApproveProcurementPlanRequest(
        String note,

        Map<UUID, Integer> itemAdjustments
) {
}

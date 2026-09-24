package com.benhsoan.adapter.inbound.rest.request.inventory;

import java.util.UUID;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record CreateProcurementPlanItemRequest(
        @NotNull(message = "Mã thuốc không được để trống.")
        UUID medicineId,

        int currentStock,

        int minStockThreshold,

        int previousPeriodConsumption,

        int suggestedQuantity,

        @Min(value = 1, message = "Số lượng thuốc đề nghị mua phải lớn hơn 0.")
        int proposedQuantity,

        String note
) {
}

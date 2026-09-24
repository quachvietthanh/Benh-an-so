package com.benhsoan.adapter.inbound.rest.request.inventory;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

public record UpdateProcurementPlanRequest(
        String note,

        @NotEmpty(message = "Danh sách thuốc trong phiếu dự trù không được để trống.")
        List<@Valid CreateProcurementPlanItemRequest> items
) {
}

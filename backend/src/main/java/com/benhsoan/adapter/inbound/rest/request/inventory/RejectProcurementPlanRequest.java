package com.benhsoan.adapter.inbound.rest.request.inventory;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RejectProcurementPlanRequest(
        @NotBlank(message = "Lý do từ chối phiếu dự trù không được để trống.")
        @Size(min = 5, message = "Lý do từ chối phải có ít nhất 5 ký tự.")
        String reason
) {
}

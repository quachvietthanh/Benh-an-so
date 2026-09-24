package com.benhsoan.adapter.inbound.rest.request.inventory;

import java.time.LocalDate;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import jakarta.validation.constraints.NotNull;

public record CreateProcurementPlanRequest(
        @NotNull(message = "Ngày bắt đầu kỳ tham chiếu không được để trống.")
        LocalDate periodStartDate,

        @NotNull(message = "Ngày kết thúc kỳ tham chiếu không được để trống.")
        LocalDate periodEndDate,

        String note,

        Boolean submitImmediately,

        @NotEmpty(message = "Danh sách thuốc trong phiếu dự trù không được để trống.")
        List<@Valid CreateProcurementPlanItemRequest> items
) {
}

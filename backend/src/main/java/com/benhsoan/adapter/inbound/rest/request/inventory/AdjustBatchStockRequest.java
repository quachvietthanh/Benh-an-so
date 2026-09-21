package com.benhsoan.adapter.inbound.rest.request.inventory;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AdjustBatchStockRequest(
        @NotNull(message = "Số lượng tồn kho thực tế không được để trống.")
        @Min(value = 0, message = "Số lượng tồn kho thực tế không được âm.")
        Integer actualQuantity,

        @NotBlank(message = "Lý do điều chỉnh không được để trống theo quy định QTN-32.")
        @Size(max = 500, message = "Lý do không được vượt quá 500 ký tự.")
        String reason
) {
}

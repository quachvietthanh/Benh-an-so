package com.benhsoan.adapter.inbound.rest.request.inventory;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DiscardExpiredBatchRequest(
        @NotBlank(message = "Lý do hủy lô không được để trống theo quy định QTN-32.")
        @Size(max = 500, message = "Lý do không được vượt quá 500 ký tự.")
        String reason
) {
}

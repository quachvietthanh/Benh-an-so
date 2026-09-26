package com.benhsoan.adapter.inbound.rest.request.inventory;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

public record CreateInventoryReceiptRequest(
        String note,

        @NotEmpty(message = "At least one receipt item is required.")
        @Valid
        List<ReceiptItemRequest> items
) {
}

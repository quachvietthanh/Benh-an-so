package com.benhsoan.adapter.inbound.rest.request.billing;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

public record AdjustInvoiceRequest(
        @NotBlank String adjustmentReason,
        @NotEmpty List<@Valid AdjustmentInvoiceLineRequest> lines
) {
}

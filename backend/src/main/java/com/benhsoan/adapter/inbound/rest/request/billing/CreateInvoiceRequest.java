package com.benhsoan.adapter.inbound.rest.request.billing;

import java.util.UUID;

public record CreateInvoiceRequest(
        UUID visitId,
        UUID paymentId
) {
}

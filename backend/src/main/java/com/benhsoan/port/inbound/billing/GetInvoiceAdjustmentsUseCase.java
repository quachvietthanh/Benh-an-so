package com.benhsoan.port.inbound.billing;

import java.util.UUID;

import com.benhsoan.port.dto.result.InvoiceAdjustmentsResult;

public interface GetInvoiceAdjustmentsUseCase {

    InvoiceAdjustmentsResult getAdjustments(UUID invoiceId);
}

package com.benhsoan.port.inbound.billing;

import java.util.UUID;

import com.benhsoan.port.dto.result.InvoiceResult;

public interface RecordInvoiceReprintUseCase {

    InvoiceResult recordReprint(UUID invoiceId);
}

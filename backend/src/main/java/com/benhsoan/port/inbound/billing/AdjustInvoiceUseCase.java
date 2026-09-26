package com.benhsoan.port.inbound.billing;

import com.benhsoan.port.dto.command.billing.AdjustInvoiceCommand;
import com.benhsoan.port.dto.result.InvoiceResult;

public interface AdjustInvoiceUseCase {

    InvoiceResult adjust(AdjustInvoiceCommand command);
}

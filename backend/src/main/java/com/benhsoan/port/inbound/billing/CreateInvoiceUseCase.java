package com.benhsoan.port.inbound.billing;

import com.benhsoan.port.dto.command.billing.CreateInvoiceCommand;
import com.benhsoan.port.dto.result.InvoiceResult;

public interface CreateInvoiceUseCase {

    InvoiceResult create(CreateInvoiceCommand command);
}

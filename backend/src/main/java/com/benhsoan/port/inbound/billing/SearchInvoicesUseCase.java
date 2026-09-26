package com.benhsoan.port.inbound.billing;

import org.springframework.data.domain.Page;

import com.benhsoan.port.dto.command.billing.SearchInvoicesQuery;
import com.benhsoan.port.dto.result.InvoiceResult;

public interface SearchInvoicesUseCase {

    Page<InvoiceResult> search(SearchInvoicesQuery query);
}

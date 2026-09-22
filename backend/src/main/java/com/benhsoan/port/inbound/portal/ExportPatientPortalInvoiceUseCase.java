package com.benhsoan.port.inbound.portal;

import java.util.UUID;

import com.benhsoan.port.dto.result.portal.InvoicePrintResult;

public interface ExportPatientPortalInvoiceUseCase {

    InvoicePrintResult export(UUID invoiceId);
}

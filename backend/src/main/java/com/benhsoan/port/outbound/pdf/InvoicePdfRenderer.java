package com.benhsoan.port.outbound.pdf;

import com.benhsoan.port.dto.result.billing.InvoicePrintDocument;

public interface InvoicePdfRenderer {

    byte[] render(InvoicePrintDocument document);
}

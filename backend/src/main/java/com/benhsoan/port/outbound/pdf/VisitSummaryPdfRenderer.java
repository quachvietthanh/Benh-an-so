package com.benhsoan.port.outbound.pdf;

import com.benhsoan.port.dto.result.VisitSummaryPrintDocument;

public interface VisitSummaryPdfRenderer {

    byte[] render(VisitSummaryPrintDocument document);
}

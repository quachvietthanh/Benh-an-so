package com.benhsoan.port.outbound.pdf;

import com.benhsoan.port.dto.result.portal.ClinicalResultPrintDocument;

public interface ClinicalResultPdfRenderer {

    byte[] render(ClinicalResultPrintDocument document);
}

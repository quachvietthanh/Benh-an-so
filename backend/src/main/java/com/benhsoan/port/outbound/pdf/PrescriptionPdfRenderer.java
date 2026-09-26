package com.benhsoan.port.outbound.pdf;

import com.benhsoan.port.dto.result.PrescriptionPrintDocument;

public interface PrescriptionPdfRenderer {

    byte[] render(PrescriptionPrintDocument document);
}

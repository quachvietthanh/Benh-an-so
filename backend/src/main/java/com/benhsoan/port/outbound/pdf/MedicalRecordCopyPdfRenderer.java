package com.benhsoan.port.outbound.pdf;

import com.benhsoan.port.dto.result.MedicalRecordCopyDocument;

public interface MedicalRecordCopyPdfRenderer {

    byte[] render(MedicalRecordCopyDocument document);
}

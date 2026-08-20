package com.benhsoan.port.inbound.prescription;

import java.util.UUID;

import com.benhsoan.port.dto.result.PrescriptionPrintResult;

public interface ExportPrescriptionUseCase {

    PrescriptionPrintResult export(UUID prescriptionId);
}

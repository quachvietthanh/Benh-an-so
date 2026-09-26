package com.benhsoan.port.inbound.prescription;

import java.util.UUID;

import com.benhsoan.port.dto.result.PrescriptionResult;

public interface GetPrescriptionUseCase {

    PrescriptionResult getById(UUID prescriptionId);
}

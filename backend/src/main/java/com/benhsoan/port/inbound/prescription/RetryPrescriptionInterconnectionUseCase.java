package com.benhsoan.port.inbound.prescription;

import java.util.UUID;

import com.benhsoan.port.dto.result.PrescriptionInterconnectionResult;

public interface RetryPrescriptionInterconnectionUseCase {

    PrescriptionInterconnectionResult retry(UUID prescriptionId);
}

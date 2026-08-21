package com.benhsoan.port.inbound.prescription;

import java.util.UUID;

import com.benhsoan.port.dto.result.PrescriptionInterconnectionResult;

public interface SendPrescriptionInterconnectionUseCase {

    PrescriptionInterconnectionResult send(UUID prescriptionId);
}

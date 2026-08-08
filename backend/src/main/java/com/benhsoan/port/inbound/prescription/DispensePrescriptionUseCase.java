package com.benhsoan.port.inbound.prescription;

import java.util.UUID;

import com.benhsoan.port.dto.result.DispensePrescriptionResult;

public interface DispensePrescriptionUseCase {

    DispensePrescriptionResult dispense(UUID prescriptionId);
}

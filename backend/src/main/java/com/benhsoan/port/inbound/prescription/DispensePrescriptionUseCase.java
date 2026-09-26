package com.benhsoan.port.inbound.prescription;

import com.benhsoan.port.dto.command.prescription.DispensePrescriptionCommand;
import com.benhsoan.port.dto.result.DispensePrescriptionResult;

public interface DispensePrescriptionUseCase {

    DispensePrescriptionResult dispense(DispensePrescriptionCommand command);
}

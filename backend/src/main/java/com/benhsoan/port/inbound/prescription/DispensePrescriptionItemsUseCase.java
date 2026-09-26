package com.benhsoan.port.inbound.prescription;

import com.benhsoan.port.dto.command.prescription.DispensePrescriptionItemsCommand;
import com.benhsoan.port.dto.result.PartialDispensePrescriptionResult;

public interface DispensePrescriptionItemsUseCase {

    PartialDispensePrescriptionResult dispense(DispensePrescriptionItemsCommand command);
}
package com.benhsoan.port.inbound.prescription;

import com.benhsoan.port.dto.command.prescription.AmendPrescriptionCommand;
import com.benhsoan.port.dto.result.PrescriptionResult;

public interface AmendPrescriptionUseCase {

    PrescriptionResult amend(AmendPrescriptionCommand command);
}

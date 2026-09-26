package com.benhsoan.port.inbound.prescription;

import com.benhsoan.port.dto.command.prescription.CancelPrescriptionCommand;
import com.benhsoan.port.dto.result.PrescriptionResult;

public interface CancelPrescriptionUseCase {

    PrescriptionResult cancel(CancelPrescriptionCommand command);
}

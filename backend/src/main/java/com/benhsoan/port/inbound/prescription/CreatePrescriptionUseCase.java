package com.benhsoan.port.inbound.prescription;

import com.benhsoan.port.dto.command.prescription.CreatePrescriptionCommand;
import com.benhsoan.port.dto.result.PrescriptionResult;

public interface CreatePrescriptionUseCase {

    PrescriptionResult create(CreatePrescriptionCommand command);
}

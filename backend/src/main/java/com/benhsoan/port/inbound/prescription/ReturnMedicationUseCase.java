package com.benhsoan.port.inbound.prescription;

import com.benhsoan.port.dto.command.prescription.ReturnMedicationCommand;
import com.benhsoan.port.dto.result.ReturnMedicationResult;

public interface ReturnMedicationUseCase {

    ReturnMedicationResult returnMedication(ReturnMedicationCommand command);
}

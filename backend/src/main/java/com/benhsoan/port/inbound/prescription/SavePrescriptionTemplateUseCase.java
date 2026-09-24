package com.benhsoan.port.inbound.prescription;

import com.benhsoan.port.dto.command.prescription.SavePrescriptionTemplateCommand;
import com.benhsoan.port.dto.result.PrescriptionTemplateResult;

public interface SavePrescriptionTemplateUseCase {

    PrescriptionTemplateResult save(SavePrescriptionTemplateCommand command);
}

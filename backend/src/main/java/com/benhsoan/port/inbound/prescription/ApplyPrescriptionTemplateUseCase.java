package com.benhsoan.port.inbound.prescription;

import com.benhsoan.port.dto.command.prescription.ApplyPrescriptionTemplateCommand;
import com.benhsoan.port.dto.result.AppliedPrescriptionTemplateResult;

public interface ApplyPrescriptionTemplateUseCase {

    AppliedPrescriptionTemplateResult apply(ApplyPrescriptionTemplateCommand command);
}

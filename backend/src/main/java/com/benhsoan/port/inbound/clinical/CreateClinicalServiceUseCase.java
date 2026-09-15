package com.benhsoan.port.inbound.clinical;

import com.benhsoan.port.dto.command.clinical.CreateClinicalServiceCommand;
import com.benhsoan.port.dto.result.ClinicalServiceManagementResult;

public interface CreateClinicalServiceUseCase {

    ClinicalServiceManagementResult create(CreateClinicalServiceCommand command);
}

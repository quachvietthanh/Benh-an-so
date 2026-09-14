package com.benhsoan.port.inbound.clinical;

import java.util.UUID;

import com.benhsoan.port.dto.command.clinical.UpdateClinicalServiceCommand;
import com.benhsoan.port.dto.result.ClinicalServiceManagementResult;

public interface UpdateClinicalServiceUseCase {

    ClinicalServiceManagementResult update(UUID clinicalServiceId, UpdateClinicalServiceCommand command);
}

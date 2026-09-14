package com.benhsoan.port.inbound.clinical;

import java.util.UUID;

import com.benhsoan.port.dto.result.ClinicalServiceManagementResult;

public interface UpdateClinicalServiceStatusUseCase {

    ClinicalServiceManagementResult updateStatus(UUID clinicalServiceId, boolean active);
}

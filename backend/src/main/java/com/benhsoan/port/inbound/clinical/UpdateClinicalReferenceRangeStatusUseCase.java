package com.benhsoan.port.inbound.clinical;

import java.util.UUID;

import com.benhsoan.port.dto.result.ClinicalReferenceRangeResult;

public interface UpdateClinicalReferenceRangeStatusUseCase {

    ClinicalReferenceRangeResult updateStatus(UUID clinicalServiceId, UUID referenceRangeId, boolean active);
}

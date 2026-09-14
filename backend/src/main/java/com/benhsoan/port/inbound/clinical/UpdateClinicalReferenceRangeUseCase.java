package com.benhsoan.port.inbound.clinical;

import java.util.UUID;

import com.benhsoan.port.dto.command.clinical.UpdateClinicalReferenceRangeCommand;
import com.benhsoan.port.dto.result.ClinicalReferenceRangeResult;

public interface UpdateClinicalReferenceRangeUseCase {

    ClinicalReferenceRangeResult update(UUID clinicalServiceId, UUID referenceRangeId,
            UpdateClinicalReferenceRangeCommand command);
}

package com.benhsoan.port.inbound.clinical;

import java.util.UUID;

import com.benhsoan.port.dto.command.clinical.CreateClinicalReferenceRangeCommand;
import com.benhsoan.port.dto.result.ClinicalReferenceRangeResult;

public interface CreateClinicalReferenceRangeUseCase {

    ClinicalReferenceRangeResult create(UUID clinicalServiceId, CreateClinicalReferenceRangeCommand command);
}

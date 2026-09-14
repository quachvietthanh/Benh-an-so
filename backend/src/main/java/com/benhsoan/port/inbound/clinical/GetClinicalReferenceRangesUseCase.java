package com.benhsoan.port.inbound.clinical;

import java.util.List;
import java.util.UUID;

import com.benhsoan.port.dto.result.ClinicalReferenceRangeResult;

public interface GetClinicalReferenceRangesUseCase {

    List<ClinicalReferenceRangeResult> list(UUID clinicalServiceId);
}

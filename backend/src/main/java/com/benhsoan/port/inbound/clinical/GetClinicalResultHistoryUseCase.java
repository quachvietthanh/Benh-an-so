package com.benhsoan.port.inbound.clinical;

import java.util.List;
import java.util.UUID;

import com.benhsoan.port.dto.result.ClinicalResultResult;

public interface GetClinicalResultHistoryUseCase {

    List<ClinicalResultResult.History> getHistory(UUID clinicalResultId);
}

package com.benhsoan.port.outbound.repository.crudRepository.clinical;

import java.util.List;
import java.util.UUID;

import com.benhsoan.domain.clinical.ClinicalResultHistory;

public interface ClinicalResultHistoryRepository {
    ClinicalResultHistory save(ClinicalResultHistory history);
    List<ClinicalResultHistory> findByClinicalResultId(UUID clinicalResultId);
}

package com.benhsoan.port.outbound.repository.clinical;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.benhsoan.domain.clinical.ClinicalReferenceRange;

public interface ClinicalReferenceRangeRepository {

    ClinicalReferenceRange save(ClinicalReferenceRange range);

    Optional<ClinicalReferenceRange> findById(UUID id);

    List<ClinicalReferenceRange> findByClinicalServiceId(UUID clinicalServiceId);

    List<ClinicalReferenceRange> findActiveByClinicalServiceId(UUID clinicalServiceId);
}

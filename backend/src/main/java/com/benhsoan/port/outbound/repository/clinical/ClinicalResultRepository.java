package com.benhsoan.port.outbound.repository.clinical;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.benhsoan.domain.clinical.ClinicalResult;
public interface ClinicalResultRepository {
    Optional<ClinicalResult> findById(UUID id);
    ClinicalResult save(ClinicalResult result);
    Optional<ClinicalResult> findByClinicalOrderItemId(UUID clinicalOrderItemId);
    Page<ClinicalResult> findByVisitId(UUID visitId, Pageable pageable);
    List<ClinicalResult> findByClinicalOrderItemIdIn(Collection<UUID> clinicalOrderItemIds);
}

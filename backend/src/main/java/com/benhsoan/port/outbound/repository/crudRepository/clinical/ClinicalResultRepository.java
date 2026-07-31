package com.benhsoan.port.outbound.repository.crudRepository.clinical;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.benhsoan.domain.clinical.ClinicalResult;
import com.benhsoan.port.outbound.repository.BaseRepository;

public interface ClinicalResultRepository extends BaseRepository<ClinicalResult, UUID> {
    Optional<ClinicalResult> findByClinicalOrderItemId(UUID clinicalOrderItemId);
    Page<ClinicalResult> findByVisitId(UUID visitId, Pageable pageable);
    List<ClinicalResult> findByClinicalOrderItemIdIn(Collection<UUID> clinicalOrderItemIds);
}

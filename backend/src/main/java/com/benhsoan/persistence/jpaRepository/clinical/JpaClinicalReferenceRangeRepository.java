package com.benhsoan.persistence.jpaRepository.clinical;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.benhsoan.persistence.entity.clinical.ClinicalReferenceRangeEntity;

public interface JpaClinicalReferenceRangeRepository extends JpaRepository<ClinicalReferenceRangeEntity, UUID> {

    List<ClinicalReferenceRangeEntity> findByClinicalServiceIdOrderByCreatedAtAscIdAsc(UUID clinicalServiceId);

    List<ClinicalReferenceRangeEntity> findByClinicalServiceIdAndActiveTrueOrderByCreatedAtAscIdAsc(UUID clinicalServiceId);
}

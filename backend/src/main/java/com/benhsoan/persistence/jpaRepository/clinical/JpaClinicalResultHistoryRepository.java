package com.benhsoan.persistence.jpaRepository.clinical;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.benhsoan.persistence.entity.clinical.ClinicalResultHistoryEntity;

public interface JpaClinicalResultHistoryRepository extends JpaRepository<ClinicalResultHistoryEntity, UUID> {
    List<ClinicalResultHistoryEntity> findByClinicalResultIdOrderByChangedAtDesc(UUID clinicalResultId);
}

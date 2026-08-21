package com.benhsoan.persistence.jpaRepository.clinical;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.benhsoan.persistence.entity.clinical.ClinicalResultHistoryEntity;

public interface JpaClinicalResultHistoryRepository extends JpaRepository<ClinicalResultHistoryEntity, UUID> {
    List<ClinicalResultHistoryEntity> findByClinicalResultIdOrderByChangedAtDesc(UUID clinicalResultId);

    @Modifying
    @Query("delete from ClinicalResultHistoryEntity history where history.clinicalResultId in :resultIds")
    void deleteByClinicalResultIdIn(@Param("resultIds") Collection<UUID> resultIds);
}

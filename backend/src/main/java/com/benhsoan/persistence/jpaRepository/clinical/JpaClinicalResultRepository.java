package com.benhsoan.persistence.jpaRepository.clinical;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.benhsoan.persistence.entity.clinical.ClinicalResultEntity;

public interface JpaClinicalResultRepository extends JpaRepository<ClinicalResultEntity, UUID> {
    Optional<ClinicalResultEntity> findByClinicalOrderItemId(UUID clinicalOrderItemId);
    Page<ClinicalResultEntity> findByVisitIdOrderByEnteredAtDesc(UUID visitId, Pageable pageable);
    List<ClinicalResultEntity> findByClinicalOrderItemIdIn(Collection<UUID> clinicalOrderItemIds);

    @Query("select result.id from ClinicalResultEntity result where result.clinicalOrderItemId in :itemIds")
    List<UUID> findIdsByClinicalOrderItemIdIn(@Param("itemIds") Collection<UUID> itemIds);

    @Modifying
    @Query("delete from ClinicalResultEntity result where result.clinicalOrderItemId in :itemIds")
    void deleteByClinicalOrderItemIdIn(@Param("itemIds") Collection<UUID> itemIds);
}

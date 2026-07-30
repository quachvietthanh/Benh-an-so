package com.benhsoan.persistence.jpaRepository.clinical;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.benhsoan.persistence.entity.clinical.ClinicalOrderItemEntity;

public interface JpaClinicalOrderItemRepository extends JpaRepository<ClinicalOrderItemEntity, UUID> {

    List<ClinicalOrderItemEntity> findByClinicalOrderIdIn(Collection<UUID> clinicalOrderIds);

    boolean existsByClinicalOrderIdAndClinicalServiceId(UUID clinicalOrderId, UUID clinicalServiceId);
}

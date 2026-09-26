package com.benhsoan.persistence.jpaRepository.inventory;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.benhsoan.persistence.entity.inventory.MedicationProcurementItemEntity;

public interface JpaMedicationProcurementItemRepository
        extends JpaRepository<MedicationProcurementItemEntity, UUID> {

    List<MedicationProcurementItemEntity> findByPlanIdOrderByCreatedAtAsc(UUID planId);

    List<MedicationProcurementItemEntity> findByPlanIdInOrderByCreatedAtAsc(Collection<UUID> planIds);

    void deleteByPlanId(UUID planId);
}

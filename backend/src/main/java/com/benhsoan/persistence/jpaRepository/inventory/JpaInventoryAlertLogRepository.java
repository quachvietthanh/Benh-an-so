package com.benhsoan.persistence.jpaRepository.inventory;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.benhsoan.domain.inventory.enums.InventoryAlertType;
import com.benhsoan.persistence.entity.inventory.InventoryAlertLogEntity;

public interface JpaInventoryAlertLogRepository
        extends JpaRepository<InventoryAlertLogEntity, UUID> {

    Optional<InventoryAlertLogEntity> findFirstByMedicineIdAndAlertTypeAndResolvedAtIsNullOrderByCreatedAtDesc(
            UUID medicineId,
            InventoryAlertType alertType
    );
}

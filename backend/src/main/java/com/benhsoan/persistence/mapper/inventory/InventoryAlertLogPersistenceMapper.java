package com.benhsoan.persistence.mapper.inventory;

import org.springframework.stereotype.Component;

import com.benhsoan.domain.inventory.InventoryAlertLog;
import com.benhsoan.persistence.entity.inventory.InventoryAlertLogEntity;

@Component
public class InventoryAlertLogPersistenceMapper {

    public InventoryAlertLog toDomain(InventoryAlertLogEntity entity) {
        if (entity == null) {
            return null;
        }
        return InventoryAlertLog.restore(
                entity.getId(),
                entity.getMedicineId(),
                entity.getAlertType(),
                entity.getThresholdValue(),
                entity.getObservedQuantity(),
                entity.getCreatedAt(),
                entity.getResolvedAt()
        );
    }

    public InventoryAlertLogEntity toEntity(InventoryAlertLog domain) {
        if (domain == null) {
            return null;
        }
        return InventoryAlertLogEntity.builder()
                .id(domain.getId())
                .medicineId(domain.getMedicineId())
                .alertType(domain.getAlertType())
                .thresholdValue(domain.getThresholdValue())
                .observedQuantity(domain.getObservedQuantity())
                .createdAt(domain.getCreatedAt())
                .resolvedAt(domain.getResolvedAt())
                .build();
    }
}

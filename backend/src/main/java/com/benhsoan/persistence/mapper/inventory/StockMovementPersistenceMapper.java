package com.benhsoan.persistence.mapper.inventory;

import org.springframework.stereotype.Component;

import com.benhsoan.domain.inventory.StockMovement;
import com.benhsoan.persistence.entity.inventory.StockMovementEntity;

@Component
public class StockMovementPersistenceMapper {

    public StockMovement toDomain(StockMovementEntity entity) {
        if (entity == null) {
            return null;
        }

        return StockMovement.restore(
                entity.getId(),
                entity.getMedicineId(),
                entity.getMedicineBatchId(),
                entity.getMovementType(),
                entity.getReferenceType(),
                entity.getReferenceId(),
                entity.getQuantityChange(),
                entity.getQuantityBefore(),
                entity.getQuantityAfter(),
                entity.getPerformedBy(),
                entity.getPerformedAt(),
                entity.getNote(),
                entity.getCreatedAt()
        );
    }

    public StockMovementEntity toEntity(StockMovement domain) {
        if (domain == null) {
            return null;
        }

        return StockMovementEntity.builder()
                .id(domain.getId())
                .medicineId(domain.getMedicineId())
                .medicineBatchId(domain.getMedicineBatchId())
                .movementType(domain.getMovementType())
                .referenceType(domain.getReferenceType())
                .referenceId(domain.getReferenceId())
                .quantityChange(domain.getQuantityChange())
                .quantityBefore(domain.getQuantityBefore())
                .quantityAfter(domain.getQuantityAfter())
                .performedBy(domain.getPerformedBy())
                .performedAt(domain.getPerformedAt())
                .note(domain.getNote())
                .createdAt(domain.getCreatedAt())
                .build();
    }
}

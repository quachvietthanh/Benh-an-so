package com.benhsoan.persistence.mapper.inventory;

import org.springframework.stereotype.Component;

import com.benhsoan.domain.inventory.MedicineBatch;
import com.benhsoan.persistence.entity.inventory.MedicineBatchEntity;

@Component
public class MedicineBatchPersistenceMapper {

    public MedicineBatch toDomain(MedicineBatchEntity entity) {
        if (entity == null) {
            return null;
        }
        return MedicineBatch.restore(
                entity.getId(),
                entity.getMedicineId(),
                entity.getBatchNumber(),
                entity.getExpiryDate(),
                entity.getQuantity(),
                entity.getStatus(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    public MedicineBatchEntity toEntity(MedicineBatch domain) {
        if (domain == null) {
            return null;
        }
        return MedicineBatchEntity.builder()
                .id(domain.getId())
                .medicineId(domain.getMedicineId())
                .batchNumber(domain.getBatchNumber())
                .expiryDate(domain.getExpiryDate())
                .quantity(domain.getQuantity())
                .status(domain.getStatus())
                .createdAt(domain.getCreatedAt())
                .updatedAt(domain.getUpdatedAt())
                .build();
    }
}

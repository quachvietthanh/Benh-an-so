package com.benhsoan.persistence.mapper.prescription;

import org.springframework.stereotype.Component;

import com.benhsoan.domain.prescription.PrescriptionDispenseItem;
import com.benhsoan.persistence.entity.prescription.PrescriptionDispenseItemEntity;

@Component
public class PrescriptionDispenseItemPersistenceMapper {

    public PrescriptionDispenseItem toDomain(PrescriptionDispenseItemEntity entity) {
        if (entity == null) {
            return null;
        }

        return PrescriptionDispenseItem.restore(
                entity.getId(),
                entity.getPrescriptionId(),
                entity.getPrescriptionItemId(),
                entity.getMedicineId(),
                entity.getMedicineBatchId(),
                entity.getDispensedQuantity(),
                entity.getDispensedBy(),
                entity.getDispensedAt(),
                entity.getCreatedAt()
        );
    }

    public PrescriptionDispenseItemEntity toEntity(PrescriptionDispenseItem domain) {
        if (domain == null) {
            return null;
        }

        return PrescriptionDispenseItemEntity.builder()
                .id(domain.getId())
                .prescriptionId(domain.getPrescriptionId())
                .prescriptionItemId(domain.getPrescriptionItemId())
                .medicineId(domain.getMedicineId())
                .medicineBatchId(domain.getMedicineBatchId())
                .dispensedQuantity(domain.getDispensedQuantity())
                .dispensedBy(domain.getDispensedBy())
                .dispensedAt(domain.getDispensedAt())
                .createdAt(domain.getCreatedAt())
                .build();
    }
}

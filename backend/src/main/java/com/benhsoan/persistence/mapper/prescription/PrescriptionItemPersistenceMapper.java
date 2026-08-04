package com.benhsoan.persistence.mapper.prescription;

import org.springframework.stereotype.Component;

import com.benhsoan.domain.prescription.PrescriptionItem;
import com.benhsoan.persistence.entity.prescription.PrescriptionItemEntity;

@Component
public class PrescriptionItemPersistenceMapper {

    public PrescriptionItem toDomain(PrescriptionItemEntity entity) {
        if (entity == null) {
            return null;
        }

        return PrescriptionItem.restore(
                entity.getId(),
                entity.getPrescriptionId(),
                entity.getMedicineId(),
                entity.getMedicineName(),
                entity.getActiveIngredient(),
                entity.getStrength(),
                entity.getUnit(),
                entity.getDosage(),
                entity.getFrequency(),
                entity.getRoute(),
                entity.getDurationDays(),
                entity.getQuantity(),
                entity.getInstructions(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    public PrescriptionItemEntity toEntity(PrescriptionItem domain) {
        if (domain == null) {
            return null;
        }

        return PrescriptionItemEntity.builder()
                .id(domain.getId())
                .prescriptionId(domain.getPrescriptionId())
                .medicineId(domain.getMedicineId())
                .medicineName(domain.getMedicineName())
                .activeIngredient(domain.getActiveIngredient())
                .strength(domain.getStrength())
                .unit(domain.getUnit())
                .dosage(domain.getDosage())
                .frequency(domain.getFrequency())
                .route(domain.getRoute())
                .durationDays(domain.getDurationDays())
                .quantity(domain.getQuantity())
                .instructions(domain.getInstructions())
                .createdAt(domain.getCreatedAt())
                .updatedAt(domain.getUpdatedAt())
                .build();
    }
}

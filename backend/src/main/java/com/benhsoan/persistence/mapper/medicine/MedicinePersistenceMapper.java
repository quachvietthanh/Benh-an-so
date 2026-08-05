package com.benhsoan.persistence.mapper.medicine;

import org.springframework.stereotype.Component;

import com.benhsoan.domain.medicine.Medicine;
import com.benhsoan.persistence.entity.medicine.MedicineEntity;

@Component
public class MedicinePersistenceMapper {

    public Medicine toDomain(MedicineEntity entity) {
        if (entity == null) {
            return null;
        }

        return Medicine.restore(
                entity.getId(),
                entity.getMedicineCode(),
                entity.getMedicineName(),
                entity.getActiveIngredient(),
                entity.getStrength(),
                entity.getDosageForm(),
                entity.getUnit(),
                entity.getDefaultRoute(),
                entity.isActive(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    public MedicineEntity toEntity(Medicine domain) {
        if (domain == null) {
            return null;
        }

        return MedicineEntity.builder()
                .id(domain.getId())
                .medicineCode(domain.getMedicineCode())
                .medicineName(domain.getMedicineName())
                .activeIngredient(domain.getActiveIngredient())
                .strength(domain.getStrength())
                .dosageForm(domain.getDosageForm())
                .unit(domain.getUnit())
                .defaultRoute(domain.getDefaultRoute())
                .active(domain.isActive())
                .createdAt(domain.getCreatedAt())
                .updatedAt(domain.getUpdatedAt())
                .build();
    }
}

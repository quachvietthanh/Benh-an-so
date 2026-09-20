package com.benhsoan.persistence.mapper.prescription;

import org.springframework.stereotype.Component;

import com.benhsoan.domain.prescription.MedicationReturn;
import com.benhsoan.persistence.entity.prescription.MedicationReturnEntity;

@Component
public class MedicationReturnPersistenceMapper {

    public MedicationReturn toDomain(MedicationReturnEntity entity) {
        if (entity == null) {
            return null;
        }

        return MedicationReturn.restore(
                entity.getId(),
                entity.getPrescriptionId(),
                entity.getPrescriptionItemId(),
                entity.getDispenseItemId(),
                entity.getMedicineId(),
                entity.getMedicineBatchId(),
                entity.getReturnedQuantity(),
                entity.getReason(),
                entity.getReturnedBy(),
                entity.getReturnedAt(),
                entity.getCreatedAt()
        );
    }

    public MedicationReturnEntity toEntity(MedicationReturn domain) {
        if (domain == null) {
            return null;
        }

        return MedicationReturnEntity.builder()
                .id(domain.getId())
                .prescriptionId(domain.getPrescriptionId())
                .prescriptionItemId(domain.getPrescriptionItemId())
                .dispenseItemId(domain.getDispenseItemId())
                .medicineId(domain.getMedicineId())
                .medicineBatchId(domain.getMedicineBatchId())
                .returnedQuantity(domain.getReturnedQuantity())
                .reason(domain.getReason())
                .returnedBy(domain.getReturnedBy())
                .returnedAt(domain.getReturnedAt())
                .createdAt(domain.getCreatedAt())
                .build();
    }
}

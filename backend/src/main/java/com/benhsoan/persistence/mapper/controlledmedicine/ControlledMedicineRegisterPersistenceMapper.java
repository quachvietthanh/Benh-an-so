package com.benhsoan.persistence.mapper.controlledmedicine;

import org.springframework.stereotype.Component;

import com.benhsoan.domain.controlledmedicine.ControlledMedicineRegister;
import com.benhsoan.persistence.entity.controlledmedicine.ControlledMedicineRegisterEntity;

@Component
public class ControlledMedicineRegisterPersistenceMapper {

    public ControlledMedicineRegister toDomain(ControlledMedicineRegisterEntity entity) {
        if (entity == null) {
            return null;
        }
        return ControlledMedicineRegister.restore(
                entity.getId(),
                entity.getPrescriptionId(),
                entity.getPrescriptionItemId(),
                entity.getMedicineId(),
                entity.getMedicineName(),
                entity.getPatientId(),
                entity.getPrescribedBy(),
                entity.getDispensedBy(),
                entity.getQuantity(),
                entity.getDispensedAt(),
                entity.getCreatedAt()
        );
    }

    public ControlledMedicineRegisterEntity toEntity(ControlledMedicineRegister domain) {
        if (domain == null) {
            return null;
        }
        return ControlledMedicineRegisterEntity.builder()
                .id(domain.getId())
                .prescriptionId(domain.getPrescriptionId())
                .prescriptionItemId(domain.getPrescriptionItemId())
                .medicineId(domain.getMedicineId())
                .medicineName(domain.getMedicineName())
                .patientId(domain.getPatientId())
                .prescribedBy(domain.getPrescribedBy())
                .dispensedBy(domain.getDispensedBy())
                .quantity(domain.getQuantity())
                .dispensedAt(domain.getDispensedAt())
                .createdAt(domain.getCreatedAt())
                .build();
    }
}

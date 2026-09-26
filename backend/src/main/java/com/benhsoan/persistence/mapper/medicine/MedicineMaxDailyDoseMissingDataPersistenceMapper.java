package com.benhsoan.persistence.mapper.medicine;

import org.springframework.stereotype.Component;

import com.benhsoan.domain.medicine.MedicineMaxDailyDoseMissingData;
import com.benhsoan.persistence.entity.medicine.MedicineMaxDailyDoseMissingDataEntity;

@Component
public class MedicineMaxDailyDoseMissingDataPersistenceMapper {

    public MedicineMaxDailyDoseMissingData toDomain(MedicineMaxDailyDoseMissingDataEntity entity) {
        if (entity == null) {
            return null;
        }
        return MedicineMaxDailyDoseMissingData.restore(
                entity.getId(),
                entity.getMedicineId(),
                entity.getActiveIngredient(),
                entity.getMissingReason(),
                entity.getFirstDetectedAt(),
                entity.getLastDetectedAt()
        );
    }

    public MedicineMaxDailyDoseMissingDataEntity toEntity(MedicineMaxDailyDoseMissingData domain) {
        if (domain == null) {
            return null;
        }
        return MedicineMaxDailyDoseMissingDataEntity.builder()
                .id(domain.getId())
                .medicineId(domain.getMedicineId())
                .activeIngredient(domain.getActiveIngredient())
                .missingReason(domain.getMissingReason())
                .firstDetectedAt(domain.getFirstDetectedAt())
                .lastDetectedAt(domain.getLastDetectedAt())
                .build();
    }
}

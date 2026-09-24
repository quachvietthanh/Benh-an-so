package com.benhsoan.persistence.jpaRepository.medicine;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.benhsoan.persistence.entity.medicine.MedicineMaxDailyDoseMissingDataEntity;

public interface JpaMedicineMaxDailyDoseMissingDataRepository
        extends JpaRepository<MedicineMaxDailyDoseMissingDataEntity, UUID> {

    Optional<MedicineMaxDailyDoseMissingDataEntity> findByMedicineIdAndMissingReason(
            UUID medicineId,
            String missingReason
    );
}

package com.benhsoan.persistence.adapterRepository.medicine;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.benhsoan.domain.medicine.MedicineMaxDailyDoseMissingData;
import com.benhsoan.persistence.entity.medicine.MedicineMaxDailyDoseMissingDataEntity;
import com.benhsoan.persistence.jpaRepository.medicine.JpaMedicineMaxDailyDoseMissingDataRepository;
import com.benhsoan.persistence.mapper.medicine.MedicineMaxDailyDoseMissingDataPersistenceMapper;
import com.benhsoan.port.outbound.repository.medicine.MedicineMaxDailyDoseMissingDataRepository;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class MedicineMaxDailyDoseMissingDataRepositoryAdapter
        implements MedicineMaxDailyDoseMissingDataRepository {

    private final JpaMedicineMaxDailyDoseMissingDataRepository jpaRepository;
    private final MedicineMaxDailyDoseMissingDataPersistenceMapper mapper;

    @Override
    public void record(
            UUID medicineId,
            String activeIngredient,
            String missingReason,
            Instant detectedAt
    ) {
        Objects.requireNonNull(medicineId, "Medicine id must not be null.");
        Objects.requireNonNull(detectedAt, "Detected time must not be null.");

        jpaRepository.findByMedicineIdAndMissingReason(medicineId, missingReason)
                .ifPresentOrElse(
                        existing -> {
                            existing.setLastDetectedAt(detectedAt);
                            jpaRepository.save(existing);
                        },
                        () -> jpaRepository.save(MedicineMaxDailyDoseMissingDataEntity.builder()
                                .id(UUID.randomUUID())
                                .medicineId(medicineId)
                                .activeIngredient(activeIngredient)
                                .missingReason(missingReason)
                                .firstDetectedAt(detectedAt)
                                .lastDetectedAt(detectedAt)
                                .build())
                );
    }

    @Override
    public List<MedicineMaxDailyDoseMissingData> findAll() {
        return jpaRepository.findAll().stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public void clear(UUID medicineId, String missingReason) {
        Objects.requireNonNull(medicineId, "Medicine id must not be null.");
        jpaRepository.findByMedicineIdAndMissingReason(medicineId, missingReason)
                .ifPresent(jpaRepository::delete);
    }
}

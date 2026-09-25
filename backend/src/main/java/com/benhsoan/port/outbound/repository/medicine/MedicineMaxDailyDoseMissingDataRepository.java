package com.benhsoan.port.outbound.repository.medicine;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.benhsoan.domain.medicine.MedicineMaxDailyDoseMissingData;

/**
 * NCL-05-CN-007: persistence for missing max-daily-dose catalog configuration.
 *
 * <p>Recording is idempotent: repeated detection of the same missing reason for
 * the same medicine updates the existing flag's {@code lastDetectedAt} instead of
 * creating duplicate rows.
 */
public interface MedicineMaxDailyDoseMissingDataRepository {

    void record(
            UUID medicineId,
            String activeIngredient,
            String missingReason,
            Instant detectedAt
    );

    List<MedicineMaxDailyDoseMissingData> findAll();

    void clear(UUID medicineId, String missingReason);
}

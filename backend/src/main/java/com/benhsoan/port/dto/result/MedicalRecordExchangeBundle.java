package com.benhsoan.port.dto.result;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Standard data exchange bundle containing multiple medical record documents (NCL-11-CN-007).
 */
public record MedicalRecordExchangeBundle(
        UUID bundleId,
        String exchangeVersion,
        Instant exportedAt,
        UUID exportedBy,
        int totalRecords,
        List<MedicalRecordExchangeDocument> records
) {
    public MedicalRecordExchangeBundle {
        records = records == null ? List.of() : List.copyOf(records);
    }
}

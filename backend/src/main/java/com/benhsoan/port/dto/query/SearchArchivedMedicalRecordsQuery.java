package com.benhsoan.port.dto.query;

import java.time.LocalDate;
import java.util.UUID;

public record SearchArchivedMedicalRecordsQuery(
        String keyword,
        LocalDate fromDate,
        LocalDate toDate,
        UUID doctorId
) {
}

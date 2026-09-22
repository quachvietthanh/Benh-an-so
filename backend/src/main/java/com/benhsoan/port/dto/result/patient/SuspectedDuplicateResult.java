package com.benhsoan.port.dto.result.patient;

import java.time.LocalDate;
import java.util.UUID;

public record SuspectedDuplicateResult(
        int rowNumber,
        String fullName,
        LocalDate dateOfBirth,
        String phone,
        String identityNumber,
        UUID matchedExistingPatientId,
        String matchedExistingPatientCode,
        String matchedExistingFullName,
        String duplicateReason
) {}

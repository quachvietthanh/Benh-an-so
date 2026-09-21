package com.benhsoan.adapter.inbound.rest.response.patient;

import java.time.LocalDate;
import java.util.UUID;

public record SuspectedDuplicateResponse(
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

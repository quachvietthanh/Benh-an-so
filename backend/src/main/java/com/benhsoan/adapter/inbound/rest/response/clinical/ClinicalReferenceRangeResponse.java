package com.benhsoan.adapter.inbound.rest.response.clinical;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.benhsoan.domain.patient.enums.Gender;

public record ClinicalReferenceRangeResponse(
        UUID id,
        UUID clinicalServiceId,
        Gender gender,
        Integer minAge,
        Integer maxAge,
        BigDecimal lowerBound,
        BigDecimal upperBound,
        boolean active,
        Instant createdAt,
        Instant updatedAt
) {
}

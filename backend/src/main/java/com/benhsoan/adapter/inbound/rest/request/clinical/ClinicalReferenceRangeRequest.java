package com.benhsoan.adapter.inbound.rest.request.clinical;

import java.math.BigDecimal;

import com.benhsoan.domain.patient.enums.Gender;

public record ClinicalReferenceRangeRequest(
        Gender gender,
        Integer minAge,
        Integer maxAge,
        BigDecimal lowerBound,
        BigDecimal upperBound
) {
}

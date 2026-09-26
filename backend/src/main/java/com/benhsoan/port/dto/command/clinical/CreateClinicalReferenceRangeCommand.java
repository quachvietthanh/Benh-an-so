package com.benhsoan.port.dto.command.clinical;

import java.math.BigDecimal;

import com.benhsoan.domain.patient.enums.Gender;

public record CreateClinicalReferenceRangeCommand(
        Gender gender,
        Integer minAge,
        Integer maxAge,
        BigDecimal lowerBound,
        BigDecimal upperBound
) {
}

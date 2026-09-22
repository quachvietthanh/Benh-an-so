package com.benhsoan.port.dto.command.billing;

import java.time.LocalDate;

import org.springframework.data.domain.Pageable;

public record PayableEncounterQuery(
        LocalDate date,
        String search,
        Pageable pageable
) {
}

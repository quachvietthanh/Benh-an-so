package com.benhsoan.port.dto.command.medicine;

import org.springframework.data.domain.Pageable;

public record SearchMedicinesQuery(
        String keyword,
        Boolean active,
        Pageable pageable
) {
}

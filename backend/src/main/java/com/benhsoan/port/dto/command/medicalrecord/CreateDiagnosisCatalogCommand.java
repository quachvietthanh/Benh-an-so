package com.benhsoan.port.dto.command.medicalrecord;

public record CreateDiagnosisCatalogCommand(
        String code,
        String name,
        String abbreviation,
        String diseaseGroup,
        String description
) {
}

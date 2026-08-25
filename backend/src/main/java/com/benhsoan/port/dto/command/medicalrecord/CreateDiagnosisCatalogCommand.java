package com.benhsoan.port.dto.command.medicalrecord;

public record CreateDiagnosisCatalogCommand(
        String code,
        String name,
        String diseaseGroup,
        String description
) {
}

package com.benhsoan.port.dto.command.medicalrecord;

import java.util.UUID;

public record UpdateDiagnosisCatalogCommand(
        UUID diagnosisCatalogId,
        String name,
        String diseaseGroup,
        String description
) {
}

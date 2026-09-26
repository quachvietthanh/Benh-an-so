package com.benhsoan.port.dto.command.specialty;

import java.util.UUID;

public record DeactivateSpecialtyCommand(
        UUID specialtyId,
        boolean confirm
) {
}

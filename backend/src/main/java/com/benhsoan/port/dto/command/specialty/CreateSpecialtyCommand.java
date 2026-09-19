package com.benhsoan.port.dto.command.specialty;

import java.util.List;
import java.util.UUID;

import lombok.Builder;

@Builder
public record CreateSpecialtyCommand(
        String code,
        String name,
        String description,
        List<UUID> doctorIds,
        List<UUID> roomIds
) {
}

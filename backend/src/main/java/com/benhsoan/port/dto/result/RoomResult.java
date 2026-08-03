package com.benhsoan.port.dto.result;

import java.time.Instant;
import java.util.UUID;

public record RoomResult(
        UUID id,
        String code,
        String name,
        boolean active,
        Instant createdAt,
        Instant updatedAt
) {
}

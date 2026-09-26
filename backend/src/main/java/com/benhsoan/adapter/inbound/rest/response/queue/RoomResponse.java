package com.benhsoan.adapter.inbound.rest.response.queue;

import java.time.Instant;
import java.util.UUID;

public record RoomResponse(
        UUID id,
        String code,
        String name,
        boolean active,
        Instant createdAt,
        Instant updatedAt
) {
}

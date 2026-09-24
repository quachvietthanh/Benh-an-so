package com.benhsoan.port.dto.command.appointment;

import java.util.UUID;

import lombok.Builder;

@Builder
public record CancelWaitlistEntryCommand(
        UUID id,
        String reason
) {
}

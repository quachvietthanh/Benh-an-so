package com.benhsoan.port.dto.command.session;

import java.util.UUID;

public record TerminateSessionCommand(
        UUID sessionId,
        String reason
) {
}
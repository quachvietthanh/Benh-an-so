package com.benhsoan.port.dto.command.auth;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;

public record TerminateSessionCommand(

        @NotNull
        UUID sessionId,

        String reason

) {
    public TerminateSessionCommand(UUID sessionId) {
        this(sessionId, null);
    }
}

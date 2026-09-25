package com.benhsoan.adapter.inbound.rest.mapper;

import org.springframework.stereotype.Component;

import com.benhsoan.adapter.inbound.rest.response.auth.ActiveSessionResponse;
import com.benhsoan.adapter.inbound.rest.response.auth.ExtendSessionResponse;
import com.benhsoan.port.dto.command.auth.TerminateSessionCommand;
import com.benhsoan.port.dto.result.auth.ActiveSessionResult;
import com.benhsoan.port.dto.result.auth.ExtendSessionResult;

import java.util.UUID;

@Component
public class SessionRestMapper {

    public ActiveSessionResponse toResponse(ActiveSessionResult result) {
        if (result == null) {
            return null;
        }

        return new ActiveSessionResponse(
                result.sessionId(),
                result.userId(),
                result.username(),
                result.fullName(),
                result.roleName(),
                result.ipAddress(),
                result.userAgent(),
                result.createdAt(),
                result.lastUsedAt(),
                result.isCurrentSession()
        );
    }

    public ExtendSessionResponse toResponse(ExtendSessionResult result) {
        if (result == null) {
            return null;
        }

        return new ExtendSessionResponse(
                result.sessionId(),
                result.lastUsedAt(),
                result.idleExpiresAt(),
                result.message()
        );
    }

    public TerminateSessionCommand toTerminateCommand(UUID sessionId) {
        return new TerminateSessionCommand(sessionId);
    }
}

package com.benhsoan.application.ucservice.session;

import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.domain.auditlog.AuditLog;
import com.benhsoan.domain.auditlog.enums.ActionType;
import com.benhsoan.domain.auditlog.enums.ResourceType;
import com.benhsoan.domain.auth.UserSession;
import com.benhsoan.domain.auth.exception.SessionExpiredException;
import com.benhsoan.domain.auth.exception.SessionNotFoundException;
import com.benhsoan.port.dto.command.session.TerminateSessionCommand;
import com.benhsoan.port.inbound.session.TerminateSessionUseCase;
import com.benhsoan.port.outbound.repository.audit.AuditLogRepository;
import com.benhsoan.port.outbound.repository.auth.UserSessionRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.benhsoan.port.outbound.time.ClockPort;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class TerminateSessionService implements TerminateSessionUseCase {

    private final UserSessionRepository userSessionRepository;
    private final CurrentUserPort currentUserPort;
    private final ClockPort clockPort;
    private final AuditLogRepository auditLogRepository;

    @Override
    public void terminate(TerminateSessionCommand command) {
        if (command == null || command.sessionId() == null) {
            throw new SessionNotFoundException();
        }

        UUID actorId = currentUserPort.getCurrentUserId();
        Instant now = clockPort.now();

        UserSession session = userSessionRepository.findById(command.sessionId())
                .orElseThrow(SessionNotFoundException::new);

        if (session.isRevoked() || session.isRefreshExpired(now)) {
            throw new SessionExpiredException();
        }

        boolean revoked = userSessionRepository.revokeById(command.sessionId(), now);
        if (!revoked) {
            // A concurrent termination already revoked the session.
            throw new SessionExpiredException();
        }

        auditLogRepository.save(AuditLog.create(
                actorId,
                ActionType.SESSION_TERMINATE,
                ResourceType.USER_SESSION,
                command.sessionId(),
                buildReasonDetail(command.reason()),
                null,
                now
        ));
    }

    private static String buildReasonDetail(String reason) {
        if (reason == null || reason.isBlank()) {
            return null;
        }
        String escaped = reason.replace("\\", "\\\\").replace("\"", "\\\"");
        return "{\"reason\":\"" + escaped + "\"}";
    }
}
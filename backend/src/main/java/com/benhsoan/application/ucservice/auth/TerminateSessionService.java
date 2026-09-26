package com.benhsoan.application.ucservice.auth;

import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.domain.auth.UserSession;
import com.benhsoan.domain.shared.exception.ValidationException;
import com.benhsoan.port.dto.command.auth.TerminateSessionCommand;
import com.benhsoan.port.inbound.auth.TerminateSessionUseCase;
import com.benhsoan.port.outbound.repository.auth.UserSessionRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.benhsoan.port.outbound.time.ClockPort;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class TerminateSessionService implements TerminateSessionUseCase {

    private final UserSessionRepository userSessionRepository;
    private final SessionAuditWriter sessionAuditWriter;
    private final CurrentUserPort currentUserPort;
    private final ClockPort clockPort;

    @Override
    public void terminateSession(TerminateSessionCommand command) {
        if (command == null || command.sessionId() == null) {
            throw new ValidationException("Session id is required.");
        }

        UserSession session = userSessionRepository.findById(command.sessionId())
                .orElseThrow(() -> new ValidationException("Session not found with id: " + command.sessionId()));

        Instant now = clockPort.now();
        UUID actorId = currentUserPort.getCurrentUserId();

        if (!session.isRevoked()) {
            session.revoke(now);
            userSessionRepository.save(session);
        }

        sessionAuditWriter.writeTerminationAudit(
                actorId,
                session.getId(),
                session.getUserId(),
                command.reason(),
                now
        );
    }
}

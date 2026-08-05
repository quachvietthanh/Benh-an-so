package com.benhsoan.application.ucservice.auth;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.domain.auditlog.AuditLog;
import com.benhsoan.domain.auditlog.enums.ActionType;
import com.benhsoan.domain.auditlog.enums.ResourceType;
import com.benhsoan.domain.auth.UserSession;
import com.benhsoan.domain.auth.exception.SessionExpiredException;
import com.benhsoan.domain.auth.exception.TokenInvalidException;
import com.benhsoan.port.dto.command.auth.LogoutCommand;
import com.benhsoan.port.inbound.auth.LogoutUseCase;
import com.benhsoan.port.outbound.authSecurity.JwtTokenPort;
import com.benhsoan.port.outbound.repository.auth.UserSessionRepository;
import com.benhsoan.port.outbound.repository.audit.AuditLogRepository;
import com.benhsoan.port.outbound.time.ClockPort;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class LogoutService implements LogoutUseCase {

    private final JwtTokenPort jwtTokenPort;

    private final UserSessionRepository userSessionRepository;

    private final AuditLogRepository auditLogRepository;

    private final ClockPort clockPort;

    @Override
    public void logout(
            LogoutCommand command
    ) {

        String accessToken = command.accessToken();

        if (!jwtTokenPort.validate(accessToken)) {
            throw new TokenInvalidException();
        }

        UserSession session =
                userSessionRepository.findById(jwtTokenPort.getSessionId(accessToken))
                .orElseThrow(TokenInvalidException::new);

        if (session.isRevoked()) {
            throw new SessionExpiredException();
        }

        if (session.isRefreshExpired(clockPort.now())) {
            throw new SessionExpiredException();
        }

        session.revoke(clockPort.now());

        userSessionRepository.save(session);

        auditLogRepository.save(
            AuditLog.create(
                session.getUserId(),
                ActionType.LOGOUT,
                ResourceType.USER_SESSION,
                session.getId(),
                null,
                null
            )
        );
    }
}

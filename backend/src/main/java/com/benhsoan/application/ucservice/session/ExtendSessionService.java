package com.benhsoan.application.ucservice.session;

import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.domain.auditlog.AuditLog;
import com.benhsoan.domain.auditlog.enums.ActionType;
import com.benhsoan.domain.auditlog.enums.ResourceType;
import com.benhsoan.domain.auth.Role;
import com.benhsoan.domain.auth.User;
import com.benhsoan.domain.auth.UserSession;
import com.benhsoan.domain.auth.exception.SessionExpiredException;
import com.benhsoan.domain.auth.exception.SessionNotFoundException;
import com.benhsoan.domain.session.SessionSettings;
import com.benhsoan.port.dto.result.session.SessionStatusResult;
import com.benhsoan.port.inbound.session.ExtendSessionUseCase;
import com.benhsoan.port.outbound.repository.audit.AuditLogRepository;
import com.benhsoan.port.outbound.repository.auth.RoleRepository;
import com.benhsoan.port.outbound.repository.auth.UserRepository;
import com.benhsoan.port.outbound.repository.auth.UserSessionRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.benhsoan.port.outbound.time.ClockPort;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class ExtendSessionService implements ExtendSessionUseCase {

    private final UserSessionRepository userSessionRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final CurrentUserPort currentUserPort;
    private final SessionConfigurationProvider sessionConfigurationProvider;
    private final SessionResultMapper resultMapper;
    private final ClockPort clockPort;
    private final AuditLogRepository auditLogRepository;

    @Override
    public SessionStatusResult extend() {
        UUID sessionId = currentUserPort.getCurrentSessionId();
        UUID userId = currentUserPort.getCurrentUserId();
        if (sessionId == null) {
            throw new SessionNotFoundException();
        }

        Instant now = clockPort.now();
        SessionSettings settings = sessionConfigurationProvider.currentSettings();

        UserSession session = userSessionRepository.findById(sessionId)
                .filter(s -> s.getUserId().equals(userId))
                .orElseThrow(SessionNotFoundException::new);

        if (!session.isActive(now, settings.inactivityTimeout())) {
            throw new SessionExpiredException();
        }

        userSessionRepository.touchLastUsed(sessionId, now);
        session.updateLastUsed(now);

        User user = userRepository.findById(userId).orElse(null);
        Role role = user == null ? null : roleRepository.findById(user.getRoleId()).orElse(null);

        auditLogRepository.save(AuditLog.create(
                userId,
                ActionType.SESSION_EXTEND,
                ResourceType.USER_SESSION,
                sessionId,
                null,
                null,
                now
        ));

        return resultMapper.toStatus(session, user, role, settings);
    }
}
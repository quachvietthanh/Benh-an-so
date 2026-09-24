package com.benhsoan.application.ucservice.session;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.domain.auth.Role;
import com.benhsoan.domain.auth.User;
import com.benhsoan.domain.auth.UserSession;
import com.benhsoan.domain.auth.exception.SessionNotFoundException;
import com.benhsoan.port.dto.result.session.SessionStatusResult;
import com.benhsoan.port.inbound.session.GetCurrentSessionUseCase;
import com.benhsoan.port.outbound.repository.auth.RoleRepository;
import com.benhsoan.port.outbound.repository.auth.UserRepository;
import com.benhsoan.port.outbound.repository.auth.UserSessionRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetCurrentSessionService implements GetCurrentSessionUseCase {

    private final UserSessionRepository userSessionRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final CurrentUserPort currentUserPort;
    private final SessionConfigurationProvider sessionConfigurationProvider;
    private final SessionResultMapper resultMapper;

    @Override
    public SessionStatusResult getCurrentSession() {
        UUID sessionId = currentUserPort.getCurrentSessionId();
        UUID userId = currentUserPort.getCurrentUserId();
        if (sessionId == null) {
            throw new SessionNotFoundException();
        }

        UserSession session = userSessionRepository.findById(sessionId)
                .filter(s -> s.getUserId().equals(userId))
                .orElseThrow(SessionNotFoundException::new);

        User user = userRepository.findById(userId).orElse(null);
        Role role = user == null ? null : roleRepository.findById(user.getRoleId()).orElse(null);

        return resultMapper.toStatus(session, user, role, sessionConfigurationProvider.currentSettings());
    }
}
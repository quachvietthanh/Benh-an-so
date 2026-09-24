package com.benhsoan.application.ucservice.session;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.domain.auth.Role;
import com.benhsoan.domain.auth.User;
import com.benhsoan.domain.auth.UserSession;
import com.benhsoan.domain.session.SessionSettings;
import com.benhsoan.port.dto.result.session.SessionSummaryResult;
import com.benhsoan.port.inbound.session.ListSessionsUseCase;
import com.benhsoan.port.outbound.repository.auth.RoleRepository;
import com.benhsoan.port.outbound.repository.auth.UserRepository;
import com.benhsoan.port.outbound.repository.auth.UserSessionRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ListSessionsService implements ListSessionsUseCase {

    private final UserSessionRepository userSessionRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final SessionConfigurationProvider sessionConfigurationProvider;
    private final SessionResultMapper resultMapper;

    @Override
    public Page<SessionSummaryResult> list(Pageable pageable) {
        Page<UserSession> sessions = userSessionRepository.findAllByRevokedAtIsNull(pageable);
        SessionSettings settings = sessionConfigurationProvider.currentSettings();

        Map<UUID, User> users = userRepository.findAllById(
                        sessions.getContent().stream()
                                .map(UserSession::getUserId)
                                .distinct()
                                .toList())
                .stream()
                .collect(Collectors.toMap(User::getId, user -> user));

        Map<UUID, Role> roles = users.values().stream()
                .map(User::getRoleId)
                .distinct()
                .map(roleRepository::findById)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toMap(Role::getId, role -> role));

        return sessions.map(session -> {
            User user = users.get(session.getUserId());
            Role role = user == null ? null : roles.get(user.getRoleId());
            return resultMapper.toSummary(session, user, role, settings);
        });
    }
}
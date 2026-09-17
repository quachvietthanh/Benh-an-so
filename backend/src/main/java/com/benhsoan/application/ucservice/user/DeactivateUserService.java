package com.benhsoan.application.ucservice.user;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.application.ucservice.auditlog.AdminOperationAuditService;
import com.benhsoan.domain.auditlog.enums.ActionType;
import com.benhsoan.domain.auditlog.enums.ResourceType;
import com.benhsoan.domain.auth.Role;
import com.benhsoan.domain.auth.User;
import com.benhsoan.domain.auth.exception.RoleNotFoundException;
import com.benhsoan.domain.auth.exception.UserNotFoundException;
import com.benhsoan.domain.shared.exception.ValidationException;
import com.benhsoan.port.dto.result.UserResult;
import com.benhsoan.port.inbound.user.DeactivateUserUseCase;
import com.benhsoan.port.outbound.repository.auth.RoleRepository;
import com.benhsoan.port.outbound.repository.auth.UserRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.benhsoan.port.outbound.time.ClockPort;

import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class DeactivateUserService implements DeactivateUserUseCase {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserResultMapper userResultMapper;
    private final AdminOperationAuditService adminOperationAuditService;
    private final CurrentUserPort currentUserPort;
    private final ClockPort clockPort;

    @Override
    public UserResult deactivate(UUID id) {

        if (id.equals(currentUserPort.getCurrentUserId())) {
            throw new ValidationException("You cannot deactivate your own account.");
        }

        User user = userRepository.findById(id)
                .orElseThrow(UserNotFoundException::new);

        boolean beforeActive = user.isActive();

        user.deactivate();

        User saved = userRepository.save(user);

        Role role = roleRepository.findById(saved.getRoleId())
                .orElseThrow(RoleNotFoundException::new);

        adminOperationAuditService.record(
                currentUserPort.getCurrentUserId(),
                ActionType.DEACTIVATE,
                ResourceType.USER,
                saved.getId(),
                AdminOperationAuditService.fields(
                        "username", saved.getUsername(),
                        "role", role.getName(),
                        "active", beforeActive),
                AdminOperationAuditService.fields(
                        "username", saved.getUsername(),
                        "role", role.getName(),
                        "active", saved.isActive()),
                clockPort.now()
        );
        return userResultMapper.toResult(user, role);
    }
}

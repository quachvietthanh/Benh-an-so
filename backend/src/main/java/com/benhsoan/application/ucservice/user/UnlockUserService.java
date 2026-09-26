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
import com.benhsoan.port.dto.result.UserResult;
import com.benhsoan.port.inbound.user.UnlockUserUseCase;
import com.benhsoan.port.outbound.authSecurity.LoginAttemptPort;
import com.benhsoan.port.outbound.repository.auth.RoleRepository;
import com.benhsoan.port.outbound.repository.auth.UserRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.benhsoan.port.outbound.time.ClockPort;

import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class UnlockUserService implements UnlockUserUseCase {

    private final UserRepository userRepository;

    private final RoleRepository roleRepository;

    private final UserResultMapper userResultMapper;

    private final LoginAttemptPort loginAttemptPort;

    private final AdminOperationAuditService adminOperationAuditService;

    private final CurrentUserPort currentUserPort;

    private final ClockPort clockPort;

    @Override
    public UserResult unlockUser(UUID id) {

        User user = userRepository.findById(id)
                .orElseThrow(UserNotFoundException::new);

        boolean wasBlocked = loginAttemptPort.isBlocked(user.getUsername());
        if (user.getPhone() != null && !user.getPhone().isBlank()) {
            wasBlocked = wasBlocked || loginAttemptPort.isBlocked(user.getPhone());
        }

        loginAttemptPort.unlock(user.getUsername());

        if (user.getPhone() != null && !user.getPhone().isBlank()) {
            loginAttemptPort.unlock(user.getPhone());
        }

        UUID adminUserId = currentUserPort.getCurrentUserId();

        adminOperationAuditService.record(
                adminUserId,
                ActionType.UNLOCK,
                ResourceType.USER,
                user.getId(),
                AdminOperationAuditService.fields("username", user.getUsername(), "locked", wasBlocked),
                AdminOperationAuditService.fields("username", user.getUsername(), "locked", false),
                clockPort.now()
        );

        Role role = roleRepository.findById(user.getRoleId())
                .orElseThrow(RoleNotFoundException::new);

        return userResultMapper.toResult(user, role);
    }
}

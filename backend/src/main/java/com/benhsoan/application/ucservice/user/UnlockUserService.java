package com.benhsoan.application.ucservice.user;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.domain.auditlog.AuditLog;
import com.benhsoan.domain.auditlog.enums.ActionType;
import com.benhsoan.domain.auditlog.enums.ResourceType;
import com.benhsoan.domain.auth.Role;
import com.benhsoan.domain.auth.User;
import com.benhsoan.domain.auth.exception.RoleNotFoundException;
import com.benhsoan.domain.auth.exception.UserNotFoundException;
import com.benhsoan.port.dto.result.UserResult;
import com.benhsoan.port.inbound.user.UnlockUserUseCase;
import com.benhsoan.port.outbound.authSecurity.LoginAttemptPort;
import com.benhsoan.port.outbound.repository.audit.AuditLogRepository;
import com.benhsoan.port.outbound.repository.auth.RoleRepository;
import com.benhsoan.port.outbound.repository.auth.UserRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;

import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class UnlockUserService implements UnlockUserUseCase {

    private final UserRepository userRepository;

    private final RoleRepository roleRepository;

    private final UserResultMapper userResultMapper;

    private final LoginAttemptPort loginAttemptPort;

    private final AuditLogRepository auditLogRepository;

    private final CurrentUserPort currentUserPort;

    @Override
    public UserResult unlockUser(UUID id) {

        User user = userRepository.findById(id)
                .orElseThrow(UserNotFoundException::new);

        loginAttemptPort.unlock(user.getUsername());

        if (user.getPhone() != null && !user.getPhone().isBlank()) {
            loginAttemptPort.unlock(user.getPhone());
        }

        UUID adminUserId = currentUserPort.getCurrentUserId();

        auditLogRepository.save(
                AuditLog.create(
                        adminUserId,
                        ActionType.UNLOCK,
                        ResourceType.USER,
                        user.getId(),
                        """
                        {
                        "unlockedUsername":"%s"
                        }
                        """.formatted(user.getUsername()),
                        null
                )
        );

        Role role = roleRepository.findById(user.getRoleId())
                .orElseThrow(RoleNotFoundException::new);

        return userResultMapper.toResult(user, role);
    }
}

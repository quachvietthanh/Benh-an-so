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
import com.benhsoan.domain.shared.exception.ValidationException;
import com.benhsoan.port.dto.result.UserResult;
import com.benhsoan.port.inbound.user.DeactivateUserUseCase;
import com.benhsoan.port.outbound.repository.crudRepository.auth.RoleRepository;
import com.benhsoan.port.outbound.repository.crudRepository.auth.UserRepository;
import com.benhsoan.port.outbound.repository.logRepository.AuditLogRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;

import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class DeactivateUserService implements DeactivateUserUseCase {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserResultMapper userResultMapper;
    private final AuditLogRepository auditLogRepository;
    private final CurrentUserPort currentUserPort;

    @Override
    public UserResult deactivate(UUID id) {

        if (id.equals(currentUserPort.getCurrentUserId())) {
            throw new ValidationException("You cannot deactivate your own account.");
        }

        User user = userRepository.findById(id)
                .orElseThrow(UserNotFoundException::new);

        user.deactivate();

        User saved = userRepository.save(user);

        Role role = roleRepository.findById(saved.getRoleId())
                .orElseThrow(RoleNotFoundException::new);

        auditLogRepository.save(
                AuditLog.create(
                        currentUserPort.getCurrentUserId(),
                        ActionType.DEACTIVATE,
                        ResourceType.USER,
                        saved.getId(),
                        """
                        {
                        "action":"DEACTIVATE",
                        "username":"%s",
                        "fullName":"%s",
                        "email":"%s",
                        "role":"%s"
                        }
                        """.formatted(
                                saved.getUsername(),
                                saved.getFullName(),
                                saved.getEmail(),
                                role.getName()),
                        null
                )
        );
        return userResultMapper.toResult(user, role);
    }
}

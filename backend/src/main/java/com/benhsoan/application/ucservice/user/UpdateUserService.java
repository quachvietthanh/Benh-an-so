package com.benhsoan.application.ucservice.user;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.domain.auditlog.AuditLog;
import com.benhsoan.domain.auditlog.enums.ActionType;
import com.benhsoan.domain.auditlog.enums.ResourceType;
import com.benhsoan.domain.auth.Role;
import com.benhsoan.domain.auth.User;
import com.benhsoan.domain.auth.exception.EmailAlreadyExistsException;
import com.benhsoan.domain.auth.exception.RoleNotFoundException;
import com.benhsoan.domain.auth.exception.UserNotFoundException;
import com.benhsoan.port.dto.command.user.UpdateUserCommand;
import com.benhsoan.port.dto.result.UserResult;
import com.benhsoan.port.inbound.user.UpdateUserUseCase;
import com.benhsoan.port.outbound.repository.auth.RoleRepository;
import com.benhsoan.port.outbound.repository.auth.UserRepository;
import com.benhsoan.port.outbound.repository.audit.AuditLogRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;

import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class UpdateUserService implements UpdateUserUseCase {

    private final UserRepository userRepository;

    private final RoleRepository roleRepository;

    private final UserResultMapper userResultMapper;

    private final AuditLogRepository auditLogRepository;

    private final CurrentUserPort currentUserPort;

    @Override
    public UserResult update(
            UUID id,
            UpdateUserCommand command
    ) {

        User user = userRepository.findById(id)
                .orElseThrow(UserNotFoundException::new);

        if (!user.getEmail().equals(command.email())
                && userRepository.existsByEmail(command.email())) {
            throw new EmailAlreadyExistsException();
        }

        Role role = roleRepository.findByName(command.roleName())
                .orElseThrow(RoleNotFoundException::new);

        user.updateProfile(
                command.fullName(),
                command.email(),
                command.phone()
        );

        user = User.restore(
                user.getId(),
                user.getUsername(),
                user.getPasswordHash(),
                user.getFullName(),
                user.getEmail(),
                user.getPhone(),
                role.getId(),
                user.isActive(),
                user.getLastLoginAt(),
                user.getCreatedAt()
        );

        User saved = userRepository.save(user);

        auditLogRepository.save(
                AuditLog.create(
                        currentUserPort.getCurrentUserId(),
                        ActionType.CREATE,
                        ResourceType.USER,
                        saved.getId(),
                        """
                        {
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

        return userResultMapper.toResult(saved, role);
    }
}
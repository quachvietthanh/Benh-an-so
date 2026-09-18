package com.benhsoan.application.ucservice.user;

import java.util.Objects;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.application.ucservice.auditlog.AdminOperationAuditService;
import com.benhsoan.domain.auditlog.enums.ActionType;
import com.benhsoan.domain.auditlog.enums.ResourceType;
import com.benhsoan.domain.auth.Role;
import com.benhsoan.domain.auth.User;
import com.benhsoan.domain.auth.exception.EmailAlreadyExistsException;
import com.benhsoan.domain.auth.exception.PhoneAlreadyExistsException;
import com.benhsoan.domain.auth.exception.RoleNotFoundException;
import com.benhsoan.domain.auth.exception.UserNotFoundException;
import com.benhsoan.port.dto.command.user.UpdateUserCommand;
import com.benhsoan.port.dto.result.UserResult;
import com.benhsoan.port.inbound.user.UpdateUserUseCase;
import com.benhsoan.port.outbound.repository.auth.RoleRepository;
import com.benhsoan.port.outbound.repository.auth.UserRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.benhsoan.port.outbound.time.ClockPort;

import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class UpdateUserService implements UpdateUserUseCase {

    private final UserRepository userRepository;

    private final RoleRepository roleRepository;

    private final UserResultMapper userResultMapper;

    private final AdminOperationAuditService adminOperationAuditService;

    private final CurrentUserPort currentUserPort;

    private final ClockPort clockPort;

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

        if (command.phone() != null
                && !command.phone().isBlank()
                && !command.phone().equals(user.getPhone())
                && userRepository.existsByPhone(command.phone())) {
            throw new PhoneAlreadyExistsException();
        }

        Role beforeRole = roleRepository.findById(user.getRoleId())
                .orElseThrow(RoleNotFoundException::new);
        String beforeFullName = user.getFullName();
        String beforeEmail = user.getEmail();
        String beforePhone = user.getPhone();

        Role role = roleRepository.findByName(command.roleName())
                .orElseThrow(RoleNotFoundException::new);

        boolean changed = !beforeFullName.equals(command.fullName())
                || !beforeEmail.equals(command.email())
                || !Objects.equals(beforePhone, command.phone())
                || !beforeRole.getId().equals(role.getId());

        if (!changed) {
            return userResultMapper.toResult(user, role);
        }

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

        adminOperationAuditService.record(
                currentUserPort.getCurrentUserId(),
                ActionType.UPDATE,
                ResourceType.USER,
                saved.getId(),
                AdminOperationAuditService.fields(
                        "username", saved.getUsername(),
                        "fullName", beforeFullName,
                        "email", beforeEmail,
                        "phone", beforePhone,
                        "role", beforeRole.getName(),
                        "active", saved.isActive()),
                AdminOperationAuditService.fields(
                        "username", saved.getUsername(),
                        "fullName", saved.getFullName(),
                        "email", saved.getEmail(),
                        "phone", saved.getPhone(),
                        "role", role.getName(),
                        "active", saved.isActive()),
                clockPort.now()
        );

        return userResultMapper.toResult(saved, role);
    }
}
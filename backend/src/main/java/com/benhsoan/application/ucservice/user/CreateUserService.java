package com.benhsoan.application.ucservice.user;

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
import com.benhsoan.domain.auth.exception.UserAlreadyExistsException;
import com.benhsoan.port.dto.command.user.CreateUserCommand;
import com.benhsoan.port.dto.result.UserResult;
import com.benhsoan.port.inbound.user.CreateUserUseCase;
import com.benhsoan.port.outbound.authSecurity.PasswordEncoderPort;
import com.benhsoan.port.outbound.repository.auth.RoleRepository;
import com.benhsoan.port.outbound.repository.auth.UserRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.benhsoan.port.outbound.time.ClockPort;

import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class CreateUserService implements CreateUserUseCase {

    private final UserRepository userRepository;

    private final RoleRepository roleRepository;

    private final PasswordEncoderPort passwordEncoder;

    private final ClockPort clockPort;

    private final UserResultMapper userResultMapper;

    private final AdminOperationAuditService adminOperationAuditService;
    
    private final CurrentUserPort currentUserPort;

    @Override
    public UserResult createUser(
            CreateUserCommand command
    ) {

        if (userRepository.existsByUsername(command.username())) {
            throw new UserAlreadyExistsException();
        }

        if (userRepository.existsByEmail(command.email())) {
            throw new EmailAlreadyExistsException();
        }

        if (command.phone() != null
                && !command.phone().isBlank()
                && userRepository.existsByPhone(command.phone())) {
            throw new PhoneAlreadyExistsException();
        }

        Role role = roleRepository.findByName(command.roleName())
                .orElseThrow(RoleNotFoundException::new);

        String passwordHash =
                passwordEncoder.encode(command.password());

        User user = User.create(
                command.username(),
                passwordHash,
                command.fullName(),
                command.email(),
                command.phone(),
                role.getId()
        );
        
        user = User.restore(
                user.getId(),
                user.getUsername(),
                user.getPasswordHash(),
                user.getFullName(),
                user.getEmail(),
                user.getPhone(),
                user.getRoleId(),
                user.isActive(),
                user.getLastLoginAt(),
                clockPort.now()
        );

        User saved = userRepository.save(user);

        adminOperationAuditService.record(
                currentUserPort.getCurrentUserId(),
                ActionType.CREATE,
                ResourceType.USER,
                saved.getId(),
                null,
                AdminOperationAuditService.fields(
                        "username", saved.getUsername(),
                        "fullName", saved.getFullName(),
                        "email", saved.getEmail(),
                        "phone", saved.getPhone(),
                        "role", role.getName(),
                        "active", saved.isActive()),
                clockPort.now()
        );

        return userResultMapper.toResult(saved, role );
    }
}
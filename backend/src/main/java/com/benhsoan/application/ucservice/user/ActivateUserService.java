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
import com.benhsoan.port.inbound.user.ActivateUserUseCase;
import com.benhsoan.port.outbound.repository.crudRepository.auth.RoleRepository;
import com.benhsoan.port.outbound.repository.crudRepository.auth.UserRepository;
import com.benhsoan.port.outbound.repository.logRepository.AuditLogRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;

import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class ActivateUserService implements ActivateUserUseCase {

    private final UserRepository userRepository;

    private final RoleRepository roleRepository;

    private final UserResultMapper userResultMapper;

    private final AuditLogRepository auditLogRepository;

    private final CurrentUserPort currentUserPort;

    @Override
    public UserResult activate(UUID id) {

        User user = userRepository.findById(id)
                .orElseThrow(UserNotFoundException::new);

        user.activate();

        User saved = userRepository.save(user);
        

        Role role = roleRepository.findById(saved.getRoleId())
                .orElseThrow(RoleNotFoundException::new);

        auditLogRepository.save(
                AuditLog.create(
                        currentUserPort.getCurrentUserId(),
                        ActionType.ACTIVATE,
                        ResourceType.USER,
                        saved.getId(),
                        """
                        {
                        "action":"ACTIVATE",
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

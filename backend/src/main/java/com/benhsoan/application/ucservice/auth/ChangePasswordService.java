package com.benhsoan.application.ucservice.auth;

import java.time.Instant;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.domain.auditlog.AuditLog;
import com.benhsoan.domain.auditlog.enums.ActionType;
import com.benhsoan.domain.auditlog.enums.ResourceType;
import com.benhsoan.domain.auth.PasswordPolicyValidator;
import com.benhsoan.domain.auth.User;
import com.benhsoan.domain.auth.exception.AccountDisabledException;
import com.benhsoan.domain.auth.exception.InvalidOldPasswordException;
import com.benhsoan.domain.auth.exception.SamePasswordException;
import com.benhsoan.domain.auth.exception.UserNotFoundException;
import com.benhsoan.port.dto.command.auth.ChangePasswordCommand;
import com.benhsoan.port.inbound.auth.ChangePasswordUseCase;
import com.benhsoan.port.outbound.authSecurity.PasswordEncoderPort;
import com.benhsoan.port.outbound.repository.audit.AuditLogRepository;
import com.benhsoan.port.outbound.repository.auth.UserRepository;
import com.benhsoan.port.outbound.repository.auth.UserSessionRepository;
import com.benhsoan.port.outbound.time.ClockPort;

import lombok.RequiredArgsConstructor;

/**
 * Service handling staff self-service password change (NCL-01-CN-005 TC-01, TC-02 / QTN-28).
 */
@Service
@RequiredArgsConstructor
@Transactional
public class ChangePasswordService implements ChangePasswordUseCase {

    private final UserRepository userRepository;
    private final UserSessionRepository userSessionRepository;
    private final PasswordEncoderPort passwordEncoderPort;
    private final AuditLogRepository auditLogRepository;
    private final ClockPort clockPort;

    @Override
    public void changePassword(ChangePasswordCommand command) {
        User user = userRepository.findById(command.userId())
                .orElseThrow(UserNotFoundException::new);

        if (!user.isActive()) {
            throw new AccountDisabledException();
        }

        if (!passwordEncoderPort.matches(command.oldPassword(), user.getPasswordHash())) {
            throw new InvalidOldPasswordException();
        }

        if (command.oldPassword().equals(command.newPassword())
                || passwordEncoderPort.matches(command.newPassword(), user.getPasswordHash())) {
            throw new SamePasswordException();
        }

        PasswordPolicyValidator.validateOrThrow(command.newPassword());

        String newPasswordHash = passwordEncoderPort.encode(command.newPassword());
        user.changePassword(newPasswordHash);
        userRepository.save(user);

        Instant now = clockPort.now();
        userSessionRepository.revokeByUserId(user.getId(), now);

        String detail = """
                {
                    "username": "%s",
                    "action": "CHANGE_PASSWORD"
                }
                """.formatted(user.getUsername()).trim();

        auditLogRepository.save(
                AuditLog.create(
                        user.getId(),
                        ActionType.CHANGE_PASSWORD,
                        ResourceType.USER,
                        user.getId(),
                        detail,
                        null,
                        now
                )
        );
    }
}

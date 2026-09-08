package com.benhsoan.application.ucservice.user;

import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.domain.auditlog.AuditLog;
import com.benhsoan.domain.auditlog.enums.ActionType;
import com.benhsoan.domain.auditlog.enums.ResourceType;
import com.benhsoan.domain.auth.PasswordPolicyValidator;
import com.benhsoan.domain.auth.TemporaryPasswordGenerator;
import com.benhsoan.domain.auth.User;
import com.benhsoan.domain.auth.exception.AccountDisabledException;
import com.benhsoan.domain.auth.exception.UserNotFoundException;
import com.benhsoan.port.dto.command.user.ResetPasswordCommand;
import com.benhsoan.port.dto.result.ResetPasswordResult;
import com.benhsoan.port.inbound.user.ResetPasswordUseCase;
import com.benhsoan.port.outbound.authSecurity.PasswordEncoderPort;
import com.benhsoan.port.outbound.repository.audit.AuditLogRepository;
import com.benhsoan.port.outbound.repository.auth.UserRepository;
import com.benhsoan.port.outbound.repository.auth.UserSessionRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.benhsoan.port.outbound.time.ClockPort;

import lombok.RequiredArgsConstructor;

/**
 * Service handling administrator reset of staff passwords (NCL-01-CN-005 TC-03 / QTN-28, QTN-31).
 */
@Service
@RequiredArgsConstructor
@Transactional
public class ResetPasswordService implements ResetPasswordUseCase {

    private final UserRepository userRepository;
    private final UserSessionRepository userSessionRepository;
    private final PasswordEncoderPort passwordEncoderPort;
    private final AuditLogRepository auditLogRepository;
    private final CurrentUserPort currentUserPort;
    private final ClockPort clockPort;

    @Override
    public ResetPasswordResult resetPassword(ResetPasswordCommand command) {
        User targetUser = userRepository.findById(command.targetUserId())
                .orElseThrow(UserNotFoundException::new);

        if (!targetUser.isActive()) {
            throw new AccountDisabledException();
        }

        String temporaryPassword;
        if (command.temporaryPassword() != null && !command.temporaryPassword().isBlank()) {
            PasswordPolicyValidator.validateOrThrow(command.temporaryPassword());
            temporaryPassword = command.temporaryPassword();
        } else {
            temporaryPassword = TemporaryPasswordGenerator.generate();
        }

        String tempHash = passwordEncoderPort.encode(temporaryPassword);
        targetUser.resetPassword(tempHash);
        userRepository.save(targetUser);

        Instant now = clockPort.now();
        userSessionRepository.revokeByUserId(targetUser.getId(), now);

        UUID actorId = currentUserPort.getCurrentUserId();

        String detail = """
                {
                    "targetUserId": "%s",
                    "targetUsername": "%s",
                    "resetBy": "%s",
                    "action": "RESET_PASSWORD"
                }
                """.formatted(targetUser.getId(), targetUser.getUsername(), actorId != null ? actorId : "SYSTEM").trim();

        auditLogRepository.save(
                AuditLog.create(
                        actorId != null ? actorId : targetUser.getId(),
                        ActionType.RESET_PASSWORD,
                        ResourceType.USER,
                        targetUser.getId(),
                        detail,
                        null,
                        now
                )
        );

        return new ResetPasswordResult(
                targetUser.getId(),
                targetUser.getUsername(),
                temporaryPassword,
                now
        );
    }
}

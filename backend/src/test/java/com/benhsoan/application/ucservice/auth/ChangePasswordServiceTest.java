package com.benhsoan.application.ucservice.auth;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.benhsoan.domain.auditlog.AuditLog;
import com.benhsoan.domain.auditlog.enums.ActionType;
import com.benhsoan.domain.auth.User;
import com.benhsoan.domain.auth.exception.AccountDisabledException;
import com.benhsoan.domain.auth.exception.InvalidOldPasswordException;
import com.benhsoan.domain.auth.exception.SamePasswordException;
import com.benhsoan.domain.auth.exception.UserNotFoundException;
import com.benhsoan.domain.auth.exception.WeakPasswordException;
import com.benhsoan.port.dto.command.auth.ChangePasswordCommand;
import com.benhsoan.port.outbound.authSecurity.PasswordEncoderPort;
import com.benhsoan.port.outbound.repository.audit.AuditLogRepository;
import com.benhsoan.port.outbound.repository.auth.UserRepository;
import com.benhsoan.port.outbound.repository.auth.UserSessionRepository;
import com.benhsoan.port.outbound.time.ClockPort;

@ExtendWith(MockitoExtension.class)
class ChangePasswordServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private UserSessionRepository userSessionRepository;
    @Mock private PasswordEncoderPort passwordEncoderPort;
    @Mock private AuditLogRepository auditLogRepository;
    @Mock private ClockPort clockPort;

    private ChangePasswordService changePasswordService;

    private final UUID userId = UUID.randomUUID();
    private final UUID roleId = UUID.randomUUID();
    private final Instant now = Instant.parse("2026-09-07T10:00:00Z");

    @BeforeEach
    void setUp() {
        changePasswordService = new ChangePasswordService(
                userRepository,
                userSessionRepository,
                passwordEncoderPort,
                auditLogRepository,
                clockPort
        );
    }

    private User createActiveUser() {
        return User.restore(
                userId,
                "doctor1",
                "$2a$10$oldHashedPassword",
                "Dr. Nguyen",
                "doctor1@benhsoan.com",
                "0901000001",
                roleId,
                true,
                true, // had mustChangePassword = true
                null,
                now
        );
    }

    @Test
    @DisplayName("throws UserNotFoundException when user does not exist")
    void userNotFoundThrowsException() {
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        ChangePasswordCommand command = new ChangePasswordCommand(userId, "OldPass123", "NewPass123");
        assertThrows(UserNotFoundException.class, () -> changePasswordService.changePassword(command));
    }

    @Test
    @DisplayName("throws AccountDisabledException when user is inactive")
    void inactiveUserThrowsException() {
        User inactiveUser = User.restore(
                userId, "doctor1", "$2a$10$hash", "Dr. Nguyen",
                "doc@test.com", null, roleId, false, null, now
        );
        when(userRepository.findById(userId)).thenReturn(Optional.of(inactiveUser));

        ChangePasswordCommand command = new ChangePasswordCommand(userId, "OldPass123", "NewPass123");
        assertThrows(AccountDisabledException.class, () -> changePasswordService.changePassword(command));
    }

    @Test
    @DisplayName("throws InvalidOldPasswordException when old password does not match")
    void wrongOldPasswordThrowsException() {
        User user = createActiveUser();
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(passwordEncoderPort.matches("WrongOldPass123", user.getPasswordHash())).thenReturn(false);

        ChangePasswordCommand command = new ChangePasswordCommand(userId, "WrongOldPass123", "NewPass123");
        assertThrows(InvalidOldPasswordException.class, () -> changePasswordService.changePassword(command));
    }

    @Test
    @DisplayName("throws SamePasswordException when new password matches old password")
    void samePasswordThrowsException() {
        User user = createActiveUser();
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(passwordEncoderPort.matches("SamePass123", user.getPasswordHash())).thenReturn(true);

        ChangePasswordCommand command = new ChangePasswordCommand(userId, "SamePass123", "SamePass123");
        assertThrows(SamePasswordException.class, () -> changePasswordService.changePassword(command));
    }

    @Test
    @DisplayName("throws WeakPasswordException when new password does not meet policy (TC-02)")
    void weakPasswordThrowsException() {
        User user = createActiveUser();
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(passwordEncoderPort.matches("OldPass123", user.getPasswordHash())).thenReturn(true);

        ChangePasswordCommand command = new ChangePasswordCommand(userId, "OldPass123", "weak");
        assertThrows(WeakPasswordException.class, () -> changePasswordService.changePassword(command));
    }

    @Test
    @DisplayName("successfully changes password, clears mustChangePassword, revokes sessions, and records audit (TC-01)")
    void changePasswordSuccess() {
        User user = createActiveUser();
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(passwordEncoderPort.matches("OldPass123", user.getPasswordHash())).thenReturn(true);
        when(passwordEncoderPort.encode("ValidNewPass123")).thenReturn("$2a$10$newHashedPassword");
        when(clockPort.now()).thenReturn(now);

        ChangePasswordCommand command = new ChangePasswordCommand(userId, "OldPass123", "ValidNewPass123");
        changePasswordService.changePassword(command);

        assertFalse(user.isMustChangePassword());
        verify(userRepository).save(user);
        verify(userSessionRepository).revokeByUserId(userId, now);
        verify(auditLogRepository).save(any(AuditLog.class));
    }
}

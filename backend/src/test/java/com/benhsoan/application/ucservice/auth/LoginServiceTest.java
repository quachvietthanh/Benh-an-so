package com.benhsoan.application.ucservice.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.benhsoan.domain.auth.Role;
import com.benhsoan.domain.auth.User;
import com.benhsoan.domain.auth.exception.InvalidCredentialsException;
import com.benhsoan.domain.auth.exception.TooManyLoginAttemptsException;
import com.benhsoan.port.dto.command.auth.LoginCommand;
import com.benhsoan.port.dto.result.LoginResult;
import com.benhsoan.port.outbound.authSecurity.JwtTokenPort;
import com.benhsoan.port.outbound.authSecurity.LoginAttemptPort;
import com.benhsoan.port.outbound.authSecurity.PasswordEncoderPort;
import com.benhsoan.port.outbound.authSecurity.RefreshTokenGeneratorPort;
import com.benhsoan.port.outbound.authSecurity.TokenHashPort;
import com.benhsoan.port.outbound.repository.audit.AuditLogRepository;
import com.benhsoan.port.outbound.repository.auth.RoleRepository;
import com.benhsoan.port.outbound.repository.auth.UserRepository;
import com.benhsoan.port.outbound.repository.auth.UserSessionRepository;
import com.benhsoan.port.outbound.time.ClockPort;

@ExtendWith(MockitoExtension.class)
class LoginServiceTest {

        private static final Instant NOW = Instant.parse("2026-03-30T10:30:00Z");
        private static final String USERNAME = "bacsi_an";
        private static final String CORRECT_PASSWORD = "CorrectPassword123!";
        private static final String WRONG_PASSWORD = "WrongPassword123!";

        @Mock
        private UserRepository userRepository;
        @Mock
        private RoleRepository roleRepository;
        @Mock
        private UserSessionRepository userSessionRepository;
        @Mock
        private PasswordEncoderPort passwordEncoderPort;
        @Mock
        private JwtTokenPort jwtTokenPort;
        @Mock
        private TokenHashPort tokenHashPort;
        @Mock
        private RefreshTokenGeneratorPort refreshTokenGeneratorPort;
        @Mock
        private LoginAttemptPort loginAttemptPort;
        @Mock
        private AuditLogRepository auditLogRepository;
        @Mock
        private LoginLockoutAuditWriter loginLockoutAuditWriter;
        @Mock
        private ClockPort clockPort;

        private LoginService loginService;

        @BeforeEach
        void setUp() {
                loginService = new LoginService(
                                userRepository,
                                roleRepository,
                                userSessionRepository,
                                passwordEncoderPort,
                                jwtTokenPort,
                                tokenHashPort,
                                refreshTokenGeneratorPort,
                                loginAttemptPort,
                                auditLogRepository,
                                loginLockoutAuditWriter,
                                clockPort);
        }

        @Test
        @DisplayName("TC-01: Nhập sai mật khẩu lần thứ 5 -> Khóa tạm ngay lập tức (HTTP 429) và ghi Audit Log LOCK")
        void tc01_wrongPasswordFifthTime_immediatelyBlocksAndRecordsLockAuditLog() {
                UUID userId = UUID.randomUUID();
                User user = mock(User.class);
                when(user.getId()).thenReturn(userId);
                when(user.getUsername()).thenReturn(USERNAME);
                when(user.isActive()).thenReturn(true);
                when(user.getPasswordHash()).thenReturn("hashed_secret");

                when(loginAttemptPort.isBlocked(USERNAME))
                                .thenReturn(false) // ban đầu chưa bị khóa
                                .thenReturn(true); // sau khi loginFailed(username) lần thứ 5 thì bị khóa

                when(loginAttemptPort.getRetryAfterSeconds(USERNAME)).thenReturn(900L);
                when(loginAttemptPort.getBlockedUntil(USERNAME)).thenReturn(NOW.plusSeconds(900));
                when(loginAttemptPort.getAttemptCount(USERNAME)).thenReturn(5);

                when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.of(user));
                when(passwordEncoderPort.matches(WRONG_PASSWORD, "hashed_secret")).thenReturn(false);

                TooManyLoginAttemptsException exception = assertThrows(
                                TooManyLoginAttemptsException.class,
                                () -> loginService.login(new LoginCommand(USERNAME, WRONG_PASSWORD)));

                assertEquals(900L, exception.getRetryAfterSeconds());
                assertEquals(NOW.plusSeconds(900), exception.getBlockedUntil());

                verify(loginAttemptPort).loginFailed(USERNAME);

                // Xác nhận gọi LoginLockoutAuditWriter để ghi nhận Audit Log LOCK an toàn trong
                // REQUIRES_NEW transaction
                verify(loginLockoutAuditWriter).writeUsernameLockout(
                                userId,
                                USERNAME,
                                5,
                                NOW.plusSeconds(900),
                                null);
        }

        @Test
        @DisplayName("TC-02: Đang trong thời gian khóa -> Nhập đúng mật khẩu vẫn bị chặn và trả về TooManyLoginAttemptsException")
        void tc02_blockedUser_cannotLoginEvenWithCorrectPassword() {
                when(loginAttemptPort.isBlocked(USERNAME)).thenReturn(true);
                when(loginAttemptPort.getRetryAfterSeconds(USERNAME)).thenReturn(750L);
                when(loginAttemptPort.getBlockedUntil(USERNAME)).thenReturn(NOW.plusSeconds(750));

                TooManyLoginAttemptsException exception = assertThrows(
                                TooManyLoginAttemptsException.class,
                                () -> loginService.login(new LoginCommand(USERNAME, CORRECT_PASSWORD)));

                assertEquals(750L, exception.getRetryAfterSeconds());
                assertEquals(NOW.plusSeconds(750), exception.getBlockedUntil());

                // Kiểm tra không đi qua bước kiểm tra DB hay mật khẩu
                verify(userRepository, never()).findByUsername(any());
                verify(passwordEncoderPort, never()).matches(any(), any());
        }

        @Test
        @DisplayName("TC-03: Nhập sai từ lần 1-4 rồi nhập đúng -> Đăng nhập thành công và reset attempt count")
        void tc03_wrongPasswordThenCorrect_succeedsAndClearsAttempts() {
                UUID userId = UUID.randomUUID();
                UUID roleId = UUID.randomUUID();
                User user = mock(User.class);
                when(user.getId()).thenReturn(userId);
                when(user.getUsername()).thenReturn(USERNAME);
                when(user.getRoleId()).thenReturn(roleId);
                when(user.isActive()).thenReturn(true);
                when(user.getPasswordHash()).thenReturn("hashed_secret");

                Role role = mock(Role.class);
                when(role.getName()).thenReturn("DOCTOR");
                when(role.getPermissions()).thenReturn(Set.of());

                when(loginAttemptPort.isBlocked(USERNAME)).thenReturn(false);
                when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.of(user));
                when(passwordEncoderPort.matches(CORRECT_PASSWORD, "hashed_secret")).thenReturn(true);
                when(roleRepository.findById(roleId)).thenReturn(Optional.of(role));
                when(clockPort.now()).thenReturn(NOW);
                when(refreshTokenGeneratorPort.generate()).thenReturn("refresh_token_value");
                when(tokenHashPort.hash(any())).thenReturn("hashed_token");
                when(jwtTokenPort.generateToken(any(), any(), any(), any(), any())).thenReturn("jwt_access_token");
                when(jwtTokenPort.getExpiredAt(any())).thenReturn(NOW.plusSeconds(3600));

                LoginResult result = loginService.login(new LoginCommand(USERNAME, CORRECT_PASSWORD));

                assertNotNull(result);
                assertEquals(USERNAME, result.username());
                assertEquals("jwt_access_token", result.accessToken());

                // Xác nhận loginSucceeded được gọi để reset đếm
                verify(loginAttemptPort).loginSucceeded(USERNAME);
        }

        @Test
        @DisplayName("Đăng nhập sai lần 1-4 -> Ném InvalidCredentialsException thông thường")
        void wrongPasswordUnderThreshold_throwsInvalidCredentials() {
                User user = mock(User.class);
                when(user.isActive()).thenReturn(true);
                when(user.getPasswordHash()).thenReturn("hashed_secret");

                when(loginAttemptPort.isBlocked(USERNAME)).thenReturn(false);
                when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.of(user));
                when(passwordEncoderPort.matches(WRONG_PASSWORD, "hashed_secret")).thenReturn(false);

                assertThrows(
                                InvalidCredentialsException.class,
                                () -> loginService.login(new LoginCommand(USERNAME, WRONG_PASSWORD)));

                verify(loginAttemptPort).loginFailed(USERNAME);
                verify(auditLogRepository, never()).save(any());
        }

        @Test
        @DisplayName("Username không tồn tại sai lần thứ 5 -> Khóa tạm nhưng không ghi AuditLog với userId null")
        void nonExistentUserFifthFailedAttempt_blocksWithoutAuditLogException() {
                when(loginAttemptPort.isBlocked(USERNAME))
                                .thenReturn(false)
                                .thenReturn(true);

                when(loginAttemptPort.getRetryAfterSeconds(USERNAME)).thenReturn(900L);
                when(loginAttemptPort.getBlockedUntil(USERNAME)).thenReturn(NOW.plusSeconds(900));

                when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.empty());

                TooManyLoginAttemptsException exception = assertThrows(
                                TooManyLoginAttemptsException.class,
                                () -> loginService.login(new LoginCommand(USERNAME, WRONG_PASSWORD)));

                assertEquals(900L, exception.getRetryAfterSeconds());
                verify(loginAttemptPort).loginFailed(USERNAME);
                verify(auditLogRepository, never()).save(any());
        }
}

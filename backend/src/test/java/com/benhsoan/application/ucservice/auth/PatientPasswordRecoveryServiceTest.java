package com.benhsoan.application.ucservice.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.benhsoan.domain.auditlog.AuditLog;
import com.benhsoan.domain.auditlog.enums.ActionType;
import com.benhsoan.domain.auditlog.enums.ResourceType;
import com.benhsoan.domain.auth.PatientPasswordRecoveryToken;
import com.benhsoan.domain.auth.Role;
import com.benhsoan.domain.auth.User;
import com.benhsoan.domain.auth.exception.AccountDisabledException;
import com.benhsoan.domain.auth.exception.InvalidVerificationCodeException;
import com.benhsoan.domain.auth.exception.SamePasswordException;
import com.benhsoan.domain.auth.exception.VerificationCodeCooldownException;
import com.benhsoan.domain.auth.exception.VerificationCodeExpiredException;
import com.benhsoan.domain.auth.exception.WeakPasswordException;
import com.benhsoan.domain.patient.Patient;
import com.benhsoan.domain.shared.exception.ValidationException;
import com.benhsoan.port.dto.command.auth.PatientForgotPasswordCommand;
import com.benhsoan.port.dto.command.auth.PatientResetPasswordCommand;
import com.benhsoan.port.dto.command.auth.PatientVerifyRecoveryCodeCommand;
import com.benhsoan.port.dto.result.PatientForgotPasswordResult;
import com.benhsoan.port.dto.result.PatientResetPasswordResult;
import com.benhsoan.port.outbound.authSecurity.PasswordEncoderPort;
import com.benhsoan.port.outbound.authSecurity.VerificationCodeGeneratorPort;
import com.benhsoan.port.outbound.notification.PatientVerificationCodePort;
import com.benhsoan.port.outbound.repository.audit.AuditLogRepository;
import com.benhsoan.port.outbound.repository.auth.PatientPasswordRecoveryTokenRepository;
import com.benhsoan.port.outbound.repository.auth.RoleRepository;
import com.benhsoan.port.outbound.repository.auth.UserRepository;
import com.benhsoan.port.outbound.repository.auth.UserSessionRepository;
import com.benhsoan.port.outbound.repository.patient.PatientRepository;
import com.benhsoan.port.outbound.time.ClockPort;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class PatientPasswordRecoveryServiceTest {

    private static final Instant NOW = Instant.parse("2026-09-17T10:00:00Z");
    private static final String PHONE = "0901111222";
    private static final String CODE = "123456";
    private static final String CODE_HASH = "hashed_123456";
    private static final String NEW_PASSWORD = "NewPassword123";
    private static final String NEW_PASSWORD_HASH = "hashed_new_password";

    @Mock private UserRepository userRepository;
    @Mock private RoleRepository roleRepository;
    @Mock private PatientRepository patientRepository;
    @Mock private UserSessionRepository userSessionRepository;
    @Mock private PatientPasswordRecoveryTokenRepository tokenRepository;
    @Mock private VerificationCodeGeneratorPort codeGeneratorPort;
    @Mock private PatientVerificationCodePort verificationCodePort;
    @Mock private PasswordEncoderPort passwordEncoderPort;
    @Mock private AuditLogRepository auditLogRepository;
    @Mock private ClockPort clockPort;
    @Mock private com.benhsoan.port.outbound.authSecurity.PatientRecoveryCooldownPort cooldownPort;
    @Mock private PatientRecoverySecurityAuditWriter auditWriter;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private PatientPasswordRecoveryService service;

    @BeforeEach
    void setUp() {
        service = new PatientPasswordRecoveryService(
                userRepository,
                roleRepository,
                patientRepository,
                userSessionRepository,
                tokenRepository,
                codeGeneratorPort,
                verificationCodePort,
                passwordEncoderPort,
                auditLogRepository,
                clockPort,
                objectMapper,
                cooldownPort,
                auditWriter
        );
    }

    // ==========================================
    // TC-01: Yêu cầu khôi phục thành công
    // ==========================================
    @Test
    void forgotPassword_registeredPatient_generatesAndSendsSimulatedCode() {
        UUID userId = UUID.randomUUID();
        UUID roleId = UUID.randomUUID();

        User user = mock(User.class);
        when(user.getId()).thenReturn(userId);
        when(user.getRoleId()).thenReturn(roleId);
        when(user.isActive()).thenReturn(true);

        Role role = mock(Role.class);
        when(role.getName()).thenReturn("PATIENT");

        when(clockPort.now()).thenReturn(NOW);
        when(userRepository.findByPhone(PHONE)).thenReturn(Optional.of(user));
        when(roleRepository.findById(roleId)).thenReturn(Optional.of(role));
        when(codeGeneratorPort.generate()).thenReturn(CODE);
        when(passwordEncoderPort.encode(CODE)).thenReturn(CODE_HASH);

        PatientForgotPasswordResult result = service.forgotPassword(
                new PatientForgotPasswordCommand(PHONE, "127.0.0.1", "Mozilla/5.0"));

        assertEquals(PatientPasswordRecoveryService.GENERIC_SUCCESS_MESSAGE, result.message());
        assertEquals(PatientPasswordRecoveryService.TTL_SECONDS, result.expiresInSeconds());

        verify(tokenRepository).save(any(PatientPasswordRecoveryToken.class));
        verify(verificationCodePort).sendVerificationCode(PHONE, CODE, PatientPasswordRecoveryService.TTL_SECONDS);
    }

    // ==========================================
    // TC-03: Bảo vệ chống rò rỉ thông tin số điện thoại (Anti-enumeration)
    // ==========================================
    @Test
    void forgotPassword_unregisteredPhone_returnsGenericSuccessMessageWithoutSendingCode() {
        when(clockPort.now()).thenReturn(NOW);
        when(userRepository.findByPhone(PHONE)).thenReturn(Optional.empty());

        PatientForgotPasswordResult result = service.forgotPassword(
                new PatientForgotPasswordCommand(PHONE, "127.0.0.1", "Mozilla/5.0"));

        assertEquals(PatientPasswordRecoveryService.GENERIC_SUCCESS_MESSAGE, result.message());
        verify(codeGeneratorPort, never()).generate();
        verify(tokenRepository, never()).save(any());
        verify(verificationCodePort, never()).sendVerificationCode(any(), any(), any(long.class));
    }

    @Test
    void forgotPassword_staffPhone_returnsGenericSuccessMessageWithoutSendingCode() {
        UUID userId = UUID.randomUUID();
        UUID roleId = UUID.randomUUID();

        User staffUser = mock(User.class);
        when(staffUser.getId()).thenReturn(userId);
        when(staffUser.getRoleId()).thenReturn(roleId);

        Role doctorRole = mock(Role.class);
        when(doctorRole.getName()).thenReturn("DOCTOR");

        when(clockPort.now()).thenReturn(NOW);
        when(userRepository.findByPhone(PHONE)).thenReturn(Optional.of(staffUser));
        when(roleRepository.findById(roleId)).thenReturn(Optional.of(doctorRole));

        PatientForgotPasswordResult result = service.forgotPassword(
                new PatientForgotPasswordCommand(PHONE, "127.0.0.1", "Mozilla/5.0"));

        assertEquals(PatientPasswordRecoveryService.GENERIC_SUCCESS_MESSAGE, result.message());
        verify(codeGeneratorPort, never()).generate();
        verify(tokenRepository, never()).save(any());
        verify(verificationCodePort, never()).sendVerificationCode(any(), any(), any(long.class));
    }

    @Test
    void forgotPassword_disabledPatientUser_returnsGenericSuccessMessageWithoutSendingCode() {
        UUID userId = UUID.randomUUID();
        UUID roleId = UUID.randomUUID();

        User user = mock(User.class);
        when(user.getId()).thenReturn(userId);
        when(user.getRoleId()).thenReturn(roleId);
        when(user.isActive()).thenReturn(false);

        Role role = mock(Role.class);
        when(role.getName()).thenReturn("PATIENT");

        when(clockPort.now()).thenReturn(NOW);
        when(userRepository.findByPhone(PHONE)).thenReturn(Optional.of(user));
        when(roleRepository.findById(roleId)).thenReturn(Optional.of(role));

        PatientForgotPasswordResult result = service.forgotPassword(
                new PatientForgotPasswordCommand(PHONE, "127.0.0.1", "Mozilla/5.0"));

        assertEquals(PatientPasswordRecoveryService.GENERIC_SUCCESS_MESSAGE, result.message());
        verify(codeGeneratorPort, never()).generate();
        verify(tokenRepository, never()).save(any());
    }

    @Test
    void forgotPassword_invalidPhoneFormat_throwsValidationException() {
        assertThrows(ValidationException.class,
                () -> service.forgotPassword(new PatientForgotPasswordCommand("invalid-phone", null, null)));
    }

    @Test
    void forgotPassword_cooldownActive_throwsVerificationCodeCooldownException() {
        when(clockPort.now()).thenReturn(NOW);
        when(cooldownPort.isInCooldown(PHONE, NOW)).thenReturn(true);
        when(cooldownPort.getRemainingCooldownSeconds(PHONE, NOW)).thenReturn(30L);

        assertThrows(VerificationCodeCooldownException.class,
                () -> service.forgotPassword(new PatientForgotPasswordCommand(PHONE, null, null)));
    }

    // ==========================================
    // TC-02: Mã xác thực hết hạn (TTL Expired)
    // ==========================================
    @Test
    void verifyCode_expiredCode_throwsVerificationCodeExpiredException() {
        when(clockPort.now()).thenReturn(NOW);

        PatientPasswordRecoveryToken expiredToken = PatientPasswordRecoveryToken.restore(
                UUID.randomUUID(), UUID.randomUUID(), PHONE, CODE_HASH,
                NOW.minusSeconds(10), // expired 10s ago
                0, null, NOW.minusSeconds(310));
        when(tokenRepository.findLatestActiveByPhone(PHONE)).thenReturn(Optional.of(expiredToken));

        assertThrows(VerificationCodeExpiredException.class,
                () -> service.verifyCode(new PatientVerifyRecoveryCodeCommand(PHONE, CODE)));
    }

    @Test
    void resetPassword_expiredCode_throwsVerificationCodeExpiredException() {
        when(clockPort.now()).thenReturn(NOW);

        PatientPasswordRecoveryToken expiredToken = PatientPasswordRecoveryToken.restore(
                UUID.randomUUID(), UUID.randomUUID(), PHONE, CODE_HASH,
                NOW.minusSeconds(10),
                0, null, NOW.minusSeconds(310));
        when(tokenRepository.findLatestActiveByPhone(PHONE)).thenReturn(Optional.of(expiredToken));

        assertThrows(VerificationCodeExpiredException.class,
                () -> service.resetPassword(new PatientResetPasswordCommand(PHONE, CODE, NEW_PASSWORD, "127.0.0.1", "agent")));
    }

    // ==========================================
    // TC-04: Đặt lại mật khẩu thành công & Kiểm toán
    // ==========================================
    @Test
    void resetPassword_validCodeAndStrongPassword_updatesPassword_revokesSessions_andAudits() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID patientId = UUID.randomUUID();
        UUID roleId = UUID.randomUUID();

        User user = mock(User.class);
        when(user.getId()).thenReturn(userId);
        when(user.getUsername()).thenReturn("patient1");
        when(user.isActive()).thenReturn(true);
        when(user.getRoleId()).thenReturn(roleId);
        when(user.getPasswordHash()).thenReturn("old_password_hash");

        Role role = mock(Role.class);
        when(role.getName()).thenReturn("PATIENT");
        when(roleRepository.findById(roleId)).thenReturn(Optional.of(role));

        Patient patient = mock(Patient.class);
        when(patient.getId()).thenReturn(patientId);

        PatientPasswordRecoveryToken token = PatientPasswordRecoveryToken.create(
                userId, PHONE, CODE_HASH, NOW.plusSeconds(300), NOW.minusSeconds(60));

        when(clockPort.now()).thenReturn(NOW);
        when(userRepository.findByPhone(PHONE)).thenReturn(Optional.of(user));
        when(tokenRepository.findLatestActiveByPhone(PHONE)).thenReturn(Optional.of(token));
        when(passwordEncoderPort.matches(CODE, CODE_HASH)).thenReturn(true);
        when(passwordEncoderPort.matches(NEW_PASSWORD, "old_password_hash")).thenReturn(false);
        when(passwordEncoderPort.encode(NEW_PASSWORD)).thenReturn(NEW_PASSWORD_HASH);
        when(patientRepository.findByUserId(userId)).thenReturn(Optional.of(patient));

        PatientResetPasswordResult result = service.resetPassword(
                new PatientResetPasswordCommand(PHONE, CODE, NEW_PASSWORD, "10.0.0.1", "MobileApp/1.0"));

        assertEquals(PatientPasswordRecoveryService.RESET_SUCCESS_MESSAGE, result.message());

        // Token marked as used
        assertTrue(token.isUsed());
        verify(tokenRepository).save(token);

        // Cooldown cleared
        verify(cooldownPort).clearCooldown(PHONE);

        // User password changed
        verify(user).changePassword(NEW_PASSWORD_HASH);
        verify(userRepository).save(user);

        // All existing sessions revoked (QTN-45)
        verify(userSessionRepository).revokeByUserId(userId, NOW);

        // Audit log recorded (QTN-31)
        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        AuditLog auditLog = captor.getValue();

        assertEquals(userId, auditLog.getUserId());
        assertEquals(ActionType.RESET_PASSWORD, auditLog.getActionType());
        assertEquals(ResourceType.PATIENT_PORTAL, auditLog.getResourceType());
        assertEquals(patientId, auditLog.getResourceId());
        assertEquals("10.0.0.1", auditLog.getIpAddress());

        JsonNode detail = objectMapper.readTree(auditLog.getDetail());
        assertEquals(PHONE, detail.get("phone").asText());
        assertEquals(patientId.toString(), detail.get("patientId").asText());
        assertEquals("PATIENT_PASSWORD_RECOVERY_SUCCESS", detail.get("action").asText());
    }

    @Test
    void resetPassword_weakPassword_throwsWeakPasswordException() {
        UUID roleId = UUID.randomUUID();
        User user = mock(User.class);
        when(user.isActive()).thenReturn(true);
        when(user.getRoleId()).thenReturn(roleId);

        Role role = mock(Role.class);
        when(role.getName()).thenReturn("PATIENT");
        when(roleRepository.findById(roleId)).thenReturn(Optional.of(role));

        PatientPasswordRecoveryToken token = PatientPasswordRecoveryToken.create(
                UUID.randomUUID(), PHONE, CODE_HASH, NOW.plusSeconds(300), NOW.minusSeconds(60));

        when(clockPort.now()).thenReturn(NOW);
        when(userRepository.findByPhone(PHONE)).thenReturn(Optional.of(user));
        when(tokenRepository.findLatestActiveByPhone(PHONE)).thenReturn(Optional.of(token));
        when(passwordEncoderPort.matches(CODE, CODE_HASH)).thenReturn(true);

        // "weak" has no uppercase, no digit, < 8 chars
        assertThrows(WeakPasswordException.class,
                () -> service.resetPassword(new PatientResetPasswordCommand(PHONE, CODE, "weak", null, null)));

        verify(user, never()).changePassword(any());
        verify(userSessionRepository, never()).revokeByUserId(any(), any());
    }

    @Test
    void resetPassword_samePasswordAsOld_throwsSamePasswordException() {
        UUID roleId = UUID.randomUUID();
        User user = mock(User.class);
        when(user.isActive()).thenReturn(true);
        when(user.getRoleId()).thenReturn(roleId);
        when(user.getPasswordHash()).thenReturn("same_hash");

        Role role = mock(Role.class);
        when(role.getName()).thenReturn("PATIENT");
        when(roleRepository.findById(roleId)).thenReturn(Optional.of(role));

        PatientPasswordRecoveryToken token = PatientPasswordRecoveryToken.create(
                UUID.randomUUID(), PHONE, CODE_HASH, NOW.plusSeconds(300), NOW.minusSeconds(60));

        when(clockPort.now()).thenReturn(NOW);
        when(userRepository.findByPhone(PHONE)).thenReturn(Optional.of(user));
        when(tokenRepository.findLatestActiveByPhone(PHONE)).thenReturn(Optional.of(token));
        when(passwordEncoderPort.matches(CODE, CODE_HASH)).thenReturn(true);
        when(passwordEncoderPort.matches(NEW_PASSWORD, "same_hash")).thenReturn(true);

        assertThrows(SamePasswordException.class,
                () -> service.resetPassword(new PatientResetPasswordCommand(PHONE, CODE, NEW_PASSWORD, null, null)));
    }

    @Test
    void resetPassword_wrongVerificationCode_incrementsAttemptsAndThrowsException() {
        PatientPasswordRecoveryToken token = PatientPasswordRecoveryToken.create(
                UUID.randomUUID(), PHONE, CODE_HASH, NOW.plusSeconds(300), NOW.minusSeconds(60));

        when(clockPort.now()).thenReturn(NOW);
        when(tokenRepository.findLatestActiveByPhone(PHONE)).thenReturn(Optional.of(token));
        when(passwordEncoderPort.matches("999999", CODE_HASH)).thenReturn(false);

        assertThrows(InvalidVerificationCodeException.class,
                () -> service.resetPassword(new PatientResetPasswordCommand(PHONE, "999999", NEW_PASSWORD, "127.0.0.1", null)));

        verify(auditWriter).recordFailedAttemptAndAudit(eq(token), any(), eq(PHONE), eq("127.0.0.1"), eq(NOW));
    }

    @Test
    void resetPassword_attemptsExceeded_throwsExceptionWithoutUpdatingPassword() {
        PatientPasswordRecoveryToken token = PatientPasswordRecoveryToken.restore(
                UUID.randomUUID(), UUID.randomUUID(), PHONE, CODE_HASH,
                NOW.plusSeconds(300),
                5, // 5 failed attempts
                null, NOW.minusSeconds(60));

        when(clockPort.now()).thenReturn(NOW);
        when(tokenRepository.findLatestActiveByPhone(PHONE)).thenReturn(Optional.of(token));

        assertThrows(InvalidVerificationCodeException.class,
                () -> service.resetPassword(new PatientResetPasswordCommand(PHONE, CODE, NEW_PASSWORD, null, null)));

        verify(passwordEncoderPort, never()).matches(any(), any());
    }

    @Test
    void verifyCode_wrongCode_recordsAttemptViaAuditWriter() {
        PatientPasswordRecoveryToken token = PatientPasswordRecoveryToken.create(
                UUID.randomUUID(), PHONE, CODE_HASH, NOW.plusSeconds(300), NOW.minusSeconds(60));

        when(clockPort.now()).thenReturn(NOW);
        when(tokenRepository.findLatestActiveByPhone(PHONE)).thenReturn(Optional.of(token));
        when(passwordEncoderPort.matches("999999", CODE_HASH)).thenReturn(false);

        assertThrows(InvalidVerificationCodeException.class,
                () -> service.verifyCode(new PatientVerifyRecoveryCodeCommand(PHONE, "999999")));

        verify(auditWriter).recordFailedAttemptAndAudit(eq(token), any(), eq(PHONE), isNull(), eq(NOW));
    }

    @Test
    void resetPassword_nonPatientRole_throwsInvalidVerificationCodeException() {
        UUID roleId = UUID.randomUUID();
        User user = mock(User.class);
        when(user.getRoleId()).thenReturn(roleId);

        Role doctorRole = mock(Role.class);
        when(doctorRole.getName()).thenReturn("DOCTOR");
        when(roleRepository.findById(roleId)).thenReturn(Optional.of(doctorRole));

        PatientPasswordRecoveryToken token = PatientPasswordRecoveryToken.create(
                UUID.randomUUID(), PHONE, CODE_HASH, NOW.plusSeconds(300), NOW.minusSeconds(60));

        when(clockPort.now()).thenReturn(NOW);
        when(tokenRepository.findLatestActiveByPhone(PHONE)).thenReturn(Optional.of(token));
        when(passwordEncoderPort.matches(CODE, CODE_HASH)).thenReturn(true);
        when(userRepository.findByPhone(PHONE)).thenReturn(Optional.of(user));

        assertThrows(InvalidVerificationCodeException.class,
                () -> service.resetPassword(new PatientResetPasswordCommand(PHONE, CODE, NEW_PASSWORD, null, null)));
    }
}

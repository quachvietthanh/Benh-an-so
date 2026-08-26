package com.benhsoan.application.ucservice.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;
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
import com.benhsoan.domain.auth.Role;
import com.benhsoan.domain.auth.User;
import com.benhsoan.domain.auth.exception.InvalidCredentialsException;
import com.benhsoan.domain.auth.exception.TooManyLoginAttemptsException;
import com.benhsoan.domain.patient.Patient;
import com.benhsoan.port.dto.command.auth.PatientLoginCommand;
import com.benhsoan.port.dto.result.PatientLoginResult;
import com.benhsoan.port.outbound.authSecurity.JwtTokenPort;
import com.benhsoan.port.outbound.authSecurity.LoginAttemptPort;
import com.benhsoan.port.outbound.authSecurity.PasswordEncoderPort;
import com.benhsoan.port.outbound.authSecurity.RefreshTokenGeneratorPort;
import com.benhsoan.port.outbound.authSecurity.TokenHashPort;
import com.benhsoan.port.outbound.repository.audit.AuditLogRepository;
import com.benhsoan.port.outbound.repository.auth.RoleRepository;
import com.benhsoan.port.outbound.repository.auth.UserRepository;
import com.benhsoan.port.outbound.repository.auth.UserSessionRepository;
import com.benhsoan.port.outbound.repository.patient.PatientRepository;
import com.benhsoan.port.outbound.time.ClockPort;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class PatientLoginServiceTest {

    private static final Instant NOW = Instant.parse("2026-08-26T02:00:00Z");
    private static final String PHONE = "0901111222";
    private static final String PASSWORD = "secret";

    @Mock private UserRepository userRepository;
    @Mock private RoleRepository roleRepository;
    @Mock private UserSessionRepository userSessionRepository;
    @Mock private PatientRepository patientRepository;
    @Mock private PasswordEncoderPort passwordEncoderPort;
    @Mock private JwtTokenPort jwtTokenPort;
    @Mock private TokenHashPort tokenHashPort;
    @Mock private RefreshTokenGeneratorPort refreshTokenGeneratorPort;
    @Mock private LoginAttemptPort loginAttemptPort;
    @Mock private AuditLogRepository auditLogRepository;
    @Mock private ClockPort clockPort;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private PatientLoginService service;

    @BeforeEach
    void setUp() {
        service = new PatientLoginService(
                userRepository, roleRepository, userSessionRepository, patientRepository,
                passwordEncoderPort, jwtTokenPort, tokenHashPort, refreshTokenGeneratorPort,
                loginAttemptPort, auditLogRepository, clockPort, objectMapper);
    }

    @Test
    void validPhoneAndPasswordReturnsTokenWithPatientIdAndRecordsLoginAudit() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID patientId = UUID.randomUUID();
        UUID roleId = UUID.randomUUID();

        User user = mock(User.class);
        when(user.getId()).thenReturn(userId);
        when(user.isActive()).thenReturn(true);
        when(user.getPasswordHash()).thenReturn("hash");
        when(user.getRoleId()).thenReturn(roleId);
        when(user.getUsername()).thenReturn("patient1");

        Role role = mock(Role.class);
        when(role.getName()).thenReturn("PATIENT");
        when(role.getPermissions()).thenReturn(Set.of());

        Patient patient = mock(Patient.class);
        when(patient.getId()).thenReturn(patientId);
        when(patient.isActive()).thenReturn(true);

        when(clockPort.now()).thenReturn(NOW);
        when(userRepository.findByPhone(PHONE)).thenReturn(Optional.of(user));
        when(passwordEncoderPort.matches(PASSWORD, "hash")).thenReturn(true);
        when(roleRepository.findById(roleId)).thenReturn(Optional.of(role));
        when(patientRepository.findByUserId(userId)).thenReturn(Optional.of(patient));
        when(refreshTokenGeneratorPort.generate()).thenReturn("refresh");
        when(tokenHashPort.hash("refresh")).thenReturn("hashed");
        when(userSessionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(jwtTokenPort.generateToken(eq(userId), any(), eq("patient1"), eq("PATIENT"), eq(Set.of()), eq(patientId)))
                .thenReturn("accessToken");
        when(jwtTokenPort.getExpiredAt("accessToken")).thenReturn(NOW.plusSeconds(900));

        PatientLoginResult result = service.login(
                new PatientLoginCommand(PHONE, PASSWORD, "127.0.0.1", "Mozilla/5.0"));

        assertEquals(patientId, result.patientId());
        assertEquals("PATIENT", result.role());
        assertEquals("accessToken", result.accessToken());

        verify(loginAttemptPort).loginSucceeded(PHONE);

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        AuditLog log = captor.getValue();
        assertEquals(ActionType.LOGIN, log.getActionType());
        assertEquals(ResourceType.PATIENT_PORTAL, log.getResourceType());
        assertEquals("127.0.0.1", log.getIpAddress());

        JsonNode node = objectMapper.readTree(log.getDetail());
        assertEquals("Mozilla/5.0", node.get("userAgent").asText());
        assertEquals("127.0.0.1", node.get("ipAddress").asText());
        assertEquals(patientId.toString(), node.get("patientId").asText());
    }

    @Test
    void wrongPasswordIncrementsFailedAttempts() {
        User user = mock(User.class);
        when(user.isActive()).thenReturn(true);
        when(user.getPasswordHash()).thenReturn("hash");

        when(userRepository.findByPhone(PHONE)).thenReturn(Optional.of(user));
        when(passwordEncoderPort.matches("wrong", "hash")).thenReturn(false);

        assertThrows(InvalidCredentialsException.class,
                () -> service.login(new PatientLoginCommand(PHONE, "wrong", null, null)));

        verify(loginAttemptPort).loginFailed(PHONE);
        verify(loginAttemptPort, never()).loginSucceeded(PHONE);
    }

    @Test
    void blockedPhoneThrowsLockoutWithRetryAfterSeconds() {
        when(loginAttemptPort.isBlocked(PHONE)).thenReturn(true);
        when(loginAttemptPort.getRetryAfterSeconds(PHONE)).thenReturn(42L);
        when(loginAttemptPort.getBlockedUntil(PHONE)).thenReturn(NOW.plusSeconds(42));

        TooManyLoginAttemptsException ex = assertThrows(
                TooManyLoginAttemptsException.class,
                () -> service.login(new PatientLoginCommand(PHONE, PASSWORD, null, null)));

        assertEquals(42L, ex.getRetryAfterSeconds());
        assertEquals(NOW.plusSeconds(42), ex.getBlockedUntil());
    }

    @Test
    void nonPatientRoleIsRejected() {
        UUID roleId = UUID.randomUUID();

        User user = mock(User.class);
        when(user.isActive()).thenReturn(true);
        when(user.getPasswordHash()).thenReturn("hash");
        when(user.getRoleId()).thenReturn(roleId);

        Role role = mock(Role.class);
        when(role.getName()).thenReturn("DOCTOR");

        when(userRepository.findByPhone(PHONE)).thenReturn(Optional.of(user));
        when(passwordEncoderPort.matches(PASSWORD, "hash")).thenReturn(true);
        when(roleRepository.findById(roleId)).thenReturn(Optional.of(role));

        assertThrows(InvalidCredentialsException.class,
                () -> service.login(new PatientLoginCommand(PHONE, PASSWORD, null, null)));
    }
}

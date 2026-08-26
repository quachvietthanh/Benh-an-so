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
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import com.benhsoan.domain.auditlog.AuditLog;
import com.benhsoan.domain.auditlog.enums.ActionType;
import com.benhsoan.domain.auditlog.enums.ResourceType;
import com.benhsoan.domain.auth.Role;
import com.benhsoan.domain.auth.User;
import com.benhsoan.domain.auth.UserSession;
import com.benhsoan.domain.auth.exception.EmailAlreadyExistsException;
import com.benhsoan.domain.auth.exception.PhoneAlreadyExistsException;
import com.benhsoan.domain.patient.Patient;
import com.benhsoan.domain.patient.enums.Gender;
import com.benhsoan.domain.patient.exception.PatientAlreadyExistsException;
import com.benhsoan.domain.shared.exception.ValidationException;
import com.benhsoan.port.dto.command.auth.PatientPortalRegistrationCommand;
import com.benhsoan.port.dto.result.PatientPortalRegistrationResult;
import com.benhsoan.port.outbound.authSecurity.JwtTokenPort;
import com.benhsoan.port.outbound.authSecurity.PasswordEncoderPort;
import com.benhsoan.port.outbound.authSecurity.RefreshTokenGeneratorPort;
import com.benhsoan.port.outbound.authSecurity.TokenHashPort;
import com.benhsoan.port.outbound.generator.PatientCodeGenerator;
import com.benhsoan.port.outbound.repository.audit.AuditLogRepository;
import com.benhsoan.port.outbound.repository.auth.RoleRepository;
import com.benhsoan.port.outbound.repository.auth.UserRepository;
import com.benhsoan.port.outbound.repository.auth.UserSessionRepository;
import com.benhsoan.port.outbound.repository.patient.PatientRepository;
import com.benhsoan.port.outbound.time.ClockPort;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class PatientPortalRegistrationServiceTest {

    private static final Instant NOW = Instant.parse("2026-08-26T02:00:00Z");
    private static final String PHONE = "0345678910";
    private static final String PASSWORD = "secret";
    private static final String FULL_NAME = "Nguyen Van A";

    @Mock private UserRepository userRepository;
    @Mock private RoleRepository roleRepository;
    @Mock private PatientRepository patientRepository;
    @Mock private PasswordEncoderPort passwordEncoderPort;
    @Mock private PatientCodeGenerator patientCodeGenerator;
    @Mock private AuditLogRepository auditLogRepository;
    @Mock private ClockPort clockPort;
    @Mock private JwtTokenPort jwtTokenPort;
    @Mock private RefreshTokenGeneratorPort refreshTokenGeneratorPort;
    @Mock private TokenHashPort tokenHashPort;
    @Mock private UserSessionRepository userSessionRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private PatientPortalRegistrationService service;

    @BeforeEach
    void setUp() {
        service = new PatientPortalRegistrationService(
                userRepository, roleRepository, patientRepository, passwordEncoderPort,
                patientCodeGenerator, auditLogRepository, clockPort, objectMapper,
                jwtTokenPort, refreshTokenGeneratorPort, tokenHashPort, userSessionRepository);
    }

    private PatientPortalRegistrationCommand command() {
        return new PatientPortalRegistrationCommand(
                PHONE, PASSWORD, FULL_NAME, LocalDate.of(1990, 1, 1), Gender.FEMALE, null, null);
    }

    private void stubTokenIssuance() {
        when(refreshTokenGeneratorPort.generate()).thenReturn("refresh-token");
        when(tokenHashPort.hash("refresh-token")).thenReturn("hashed-refresh");
        when(userSessionRepository.save(any(UserSession.class))).thenAnswer(inv -> inv.getArgument(0));
        when(jwtTokenPort.generateToken(any(), any(), any(), any(), any(), any())).thenReturn("access-token");
    }

    @Test
    void linksExistingPatientWhenPhoneMatches() throws Exception {
        UUID roleId = UUID.randomUUID();
        UUID patientId = UUID.randomUUID();

        Role role = mock(Role.class);
        when(role.getId()).thenReturn(roleId);
        when(role.getPermissions()).thenReturn(Set.of());

        Patient existing = mock(Patient.class);
        when(existing.getUserId()).thenReturn(null);
        when(existing.getId()).thenReturn(patientId);
        when(existing.getFullName()).thenReturn(FULL_NAME);

        when(userRepository.existsByPhone(PHONE)).thenReturn(false);
        when(userRepository.existsByEmail(PHONE + "@benhsoan.com")).thenReturn(false);
        when(roleRepository.findByName("PATIENT")).thenReturn(Optional.of(role));
        when(passwordEncoderPort.encode(PASSWORD)).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(patientRepository.findAllByPhone(PHONE)).thenReturn(List.of(existing));
        when(patientRepository.save(any(Patient.class))).thenAnswer(inv -> inv.getArgument(0));
        when(clockPort.now()).thenReturn(NOW);
        stubTokenIssuance();

        PatientPortalRegistrationResult result = service.register(command());

        assertEquals(patientId, result.patientId());
        assertEquals(PHONE, result.phone());
        assertNotNull(result.accessToken());
        assertNotNull(result.refreshToken());
        assertEquals("Bearer", result.tokenType());

        verify(existing).linkUser(any(UUID.class));
        verify(patientRepository).save(existing);
    }

    @Test
    void createsNewPatientWhenPhoneNotRegistered() throws Exception {
        UUID roleId = UUID.randomUUID();

        Role role = mock(Role.class);
        when(role.getId()).thenReturn(roleId);
        when(role.getPermissions()).thenReturn(Set.of());

        when(userRepository.existsByPhone(PHONE)).thenReturn(false);
        when(userRepository.existsByEmail(PHONE + "@benhsoan.com")).thenReturn(false);
        when(roleRepository.findByName("PATIENT")).thenReturn(Optional.of(role));
        when(passwordEncoderPort.encode(PASSWORD)).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(patientRepository.findAllByPhone(PHONE)).thenReturn(List.of());
        when(patientCodeGenerator.generate()).thenReturn("BN000123");
        when(patientRepository.save(any(Patient.class))).thenAnswer(inv -> inv.getArgument(0));
        when(clockPort.now()).thenReturn(NOW);
        stubTokenIssuance();

        PatientPortalRegistrationResult result = service.register(command());

        assertNotNull(result.patientId());
        assertNotNull(result.accessToken());
        assertNotNull(result.refreshToken());

        ArgumentCaptor<Patient> captor = ArgumentCaptor.forClass(Patient.class);
        verify(patientRepository).save(captor.capture());
        Patient created = captor.getValue();
        assertEquals("BN000123", created.getPatientCode());
        assertNotNull(created.getUserId());
        assertEquals(PHONE, created.getPhone());
    }

    @Test
    void rejectsDuplicatePhoneWithConflict() {
        when(userRepository.existsByPhone(PHONE)).thenReturn(true);

        PhoneAlreadyExistsException ex = assertThrows(
                PhoneAlreadyExistsException.class,
                () -> service.register(command()));

        assertEquals("Số điện thoại đã được đăng ký tài khoản. Vui lòng đăng nhập.", ex.getMessage());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void rejectsInvalidPhoneFormat() {
        assertThrows(ValidationException.class,
                () -> service.register(new PatientPortalRegistrationCommand(
                        "12345", PASSWORD, FULL_NAME, LocalDate.of(1990, 1, 1), Gender.FEMALE, null, null)));
    }

    @Test
    void recordsRegistrationAuditLog() throws Exception {
        UUID roleId = UUID.randomUUID();
        UUID patientId = UUID.randomUUID();

        Role role = mock(Role.class);
        when(role.getId()).thenReturn(roleId);
        when(role.getPermissions()).thenReturn(Set.of());

        Patient existing = mock(Patient.class);
        when(existing.getUserId()).thenReturn(null);
        when(existing.getId()).thenReturn(patientId);
        when(existing.getFullName()).thenReturn(FULL_NAME);

        when(userRepository.existsByPhone(PHONE)).thenReturn(false);
        when(userRepository.existsByEmail(PHONE + "@benhsoan.com")).thenReturn(false);
        when(roleRepository.findByName("PATIENT")).thenReturn(Optional.of(role));
        when(passwordEncoderPort.encode(PASSWORD)).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(patientRepository.findAllByPhone(PHONE)).thenReturn(List.of(existing));
        when(patientRepository.save(any(Patient.class))).thenAnswer(inv -> inv.getArgument(0));
        when(clockPort.now()).thenReturn(NOW);
        stubTokenIssuance();

        service.register(command());

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        AuditLog log = captor.getValue();
        assertEquals(ActionType.CREATE, log.getActionType());
        assertEquals(ResourceType.PATIENT_PORTAL, log.getResourceType());
        assertEquals(patientId, log.getResourceId());

        JsonNode node = objectMapper.readTree(log.getDetail());
        assertEquals("SELF_PORTAL_REGISTRATION", node.get("method").asText());
        assertEquals(PHONE, node.get("phone").asText());
        assertEquals(patientId.toString(), node.get("patientId").asText());
        assertEquals(NOW.toString(), node.get("registeredAt").asText());
    }

    @Test
    void normalizesPlus84PhoneAndLinksCandidate() {
        UUID roleId = UUID.randomUUID();
        UUID patientId = UUID.randomUUID();

        Role role = mock(Role.class);
        when(role.getId()).thenReturn(roleId);
        when(role.getPermissions()).thenReturn(Set.of());

        Patient existing = mock(Patient.class);
        when(existing.getUserId()).thenReturn(null);
        when(existing.getId()).thenReturn(patientId);
        when(existing.getFullName()).thenReturn(FULL_NAME);

        when(userRepository.existsByPhone(PHONE)).thenReturn(false);
        when(userRepository.existsByEmail(PHONE + "@benhsoan.com")).thenReturn(false);
        when(roleRepository.findByName("PATIENT")).thenReturn(Optional.of(role));
        when(passwordEncoderPort.encode(PASSWORD)).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(patientRepository.findAllByPhone(PHONE)).thenReturn(List.of(existing));
        when(patientRepository.save(any(Patient.class))).thenAnswer(inv -> inv.getArgument(0));
        when(clockPort.now()).thenReturn(NOW);
        stubTokenIssuance();

        PatientPortalRegistrationResult result = service.register(
                new PatientPortalRegistrationCommand(
                        "+84345678910", PASSWORD, FULL_NAME,
                        LocalDate.of(1990, 1, 1), Gender.FEMALE, null, null));

        assertEquals(patientId, result.patientId());
        assertEquals(PHONE, result.phone());
        verify(existing).linkUser(any(UUID.class));
    }

    @Test
    void rejectsDuplicateIdentityNumberWithConflict() {
        when(userRepository.existsByPhone(PHONE)).thenReturn(false);
        when(patientRepository.existsByIdentityNumber("001122334455")).thenReturn(true);

        assertThrows(PatientAlreadyExistsException.class,
                () -> service.register(new PatientPortalRegistrationCommand(
                        PHONE, PASSWORD, FULL_NAME,
                        LocalDate.of(1990, 1, 1), Gender.FEMALE, "001122334455", null)));
    }

    @Test
    void resolvesMultipleCandidatesDeterministically() {
        UUID roleId = UUID.randomUUID();

        Role role = mock(Role.class);
        when(role.getId()).thenReturn(roleId);
        when(role.getPermissions()).thenReturn(Set.of());

        Patient byIdentity = mock(Patient.class);
        when(byIdentity.getUserId()).thenReturn(null);
        when(byIdentity.getIdentityNumber()).thenReturn("001122334455");
        when(byIdentity.getId()).thenReturn(UUID.randomUUID());
        when(byIdentity.getFullName()).thenReturn(FULL_NAME);

        Patient other = mock(Patient.class);
        when(other.getUserId()).thenReturn(null);
        when(other.getIdentityNumber()).thenReturn(null);

        when(userRepository.existsByPhone(PHONE)).thenReturn(false);
        when(userRepository.existsByEmail(PHONE + "@benhsoan.com")).thenReturn(false);
        when(roleRepository.findByName("PATIENT")).thenReturn(Optional.of(role));
        when(passwordEncoderPort.encode(PASSWORD)).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(patientRepository.findAllByPhone(PHONE)).thenReturn(List.of(other, byIdentity));
        when(patientRepository.save(any(Patient.class))).thenAnswer(inv -> inv.getArgument(0));
        when(clockPort.now()).thenReturn(NOW);
        stubTokenIssuance();

        service.register(new PatientPortalRegistrationCommand(
                PHONE, PASSWORD, FULL_NAME,
                LocalDate.of(1990, 1, 1), Gender.FEMALE, "001122334455", null));

        verify(byIdentity).linkUser(any(UUID.class));
        verify(other, never()).linkUser(any(UUID.class));
    }

    @Test
    void mapsDataIntegrityViolationToConflict() {
        UUID roleId = UUID.randomUUID();

        Role role = mock(Role.class);
        when(role.getId()).thenReturn(roleId);

        when(userRepository.existsByPhone(PHONE)).thenReturn(false);
        when(roleRepository.findByName("PATIENT")).thenReturn(Optional.of(role));
        when(passwordEncoderPort.encode(PASSWORD)).thenReturn("hashed");
        when(userRepository.save(any(User.class)))
                .thenThrow(new DataIntegrityViolationException("uk_users_phone"));

        assertThrows(PhoneAlreadyExistsException.class, () -> service.register(command()));
    }

    @Test
    void registersWithCustomEmail() {
        UUID roleId = UUID.randomUUID();

        Role role = mock(Role.class);
        when(role.getId()).thenReturn(roleId);
        when(role.getPermissions()).thenReturn(Set.of());

        when(userRepository.existsByPhone(PHONE)).thenReturn(false);
        when(userRepository.existsByEmail("patient@example.com")).thenReturn(false);
        when(roleRepository.findByName("PATIENT")).thenReturn(Optional.of(role));
        when(passwordEncoderPort.encode(PASSWORD)).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(patientRepository.findAllByPhone(PHONE)).thenReturn(List.of());
        when(patientCodeGenerator.generate()).thenReturn("BN000123");
        when(patientRepository.save(any(Patient.class))).thenAnswer(inv -> inv.getArgument(0));
        when(clockPort.now()).thenReturn(NOW);
        stubTokenIssuance();

        PatientPortalRegistrationResult result = service.register(
                new PatientPortalRegistrationCommand(
                        PHONE, PASSWORD, FULL_NAME,
                        LocalDate.of(1990, 1, 1), Gender.FEMALE, null, "patient@example.com"));

        assertNotNull(result.accessToken());
        assertNotNull(result.refreshToken());

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertEquals("patient@example.com", captor.getValue().getEmail());
    }

    @Test
    void rejectsDuplicateCustomEmailWithConflict() {
        when(userRepository.existsByPhone(PHONE)).thenReturn(false);
        when(userRepository.existsByEmail("patient@example.com")).thenReturn(true);

        assertThrows(EmailAlreadyExistsException.class,
                () -> service.register(new PatientPortalRegistrationCommand(
                        PHONE, PASSWORD, FULL_NAME,
                        LocalDate.of(1990, 1, 1), Gender.FEMALE, null, "patient@example.com")));

        verify(userRepository, never()).save(any(User.class));
    }
}

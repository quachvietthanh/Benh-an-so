package com.benhsoan.application.ucservice.auth;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import com.benhsoan.application.ucservice.auditlog.AdminOperationAuditService;
import com.benhsoan.domain.auth.Role;
import com.benhsoan.domain.auth.exception.RoleNotFoundException;
import com.benhsoan.domain.shared.exception.ValidationException;
import com.benhsoan.port.dto.command.auth.ConfigureTwoFactorAuthenticationCommand;
import com.benhsoan.port.dto.result.TwoFactorConfigurationResult;
import com.benhsoan.port.outbound.repository.auth.RoleRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.benhsoan.port.outbound.time.ClockPort;

@ExtendWith(MockitoExtension.class)
class ConfigureTwoFactorAuthenticationServiceTest {

    private static final Instant NOW = Instant.parse("2026-09-22T10:00:00Z");
    private static final UUID ADMIN_ID = UUID.randomUUID();
    private static final UUID ROLE_ID = UUID.randomUUID();

    @Mock private RoleRepository roleRepository;
    @Mock private CurrentUserPort currentUserPort;
    @Mock private AdminOperationAuditService adminOperationAuditService;
    @Mock private ClockPort clockPort;

    private ConfigureTwoFactorAuthenticationService service;

    @BeforeEach
    void setUp() {
        service = new ConfigureTwoFactorAuthenticationService(
                roleRepository, currentUserPort, adminOperationAuditService, clockPort);
    }

    @Test
    void enableTwoFactorForDoctor_updatesRoleAndAudits() {
        Role doctor = Role.restore(ROLE_ID, "DOCTOR", null, true, NOW, NOW, Set.of());
        when(currentUserPort.hasPermission("TWO_FACTOR_AUTH_MANAGE")).thenReturn(true);
        when(currentUserPort.getCurrentUserId()).thenReturn(ADMIN_ID);
        when(clockPort.now()).thenReturn(NOW);
        when(roleRepository.findByName("DOCTOR")).thenReturn(Optional.of(doctor));
        when(roleRepository.save(doctor)).thenReturn(doctor);

        TwoFactorConfigurationResult result = service.configure(new ConfigureTwoFactorAuthenticationCommand("doctor", true));

        assertTrue(result.twoFactorRequired());
        assertTrue(doctor.isTwoFactorRequired());
        verify(adminOperationAuditService).record(eq(ADMIN_ID), any(), any(), eq(ROLE_ID), any(), any(), eq(NOW));
    }

    @Test
    void enableTwoFactorForAdmin_updatesRoleAndAudits() {
        Role admin = Role.restore(ROLE_ID, "ADMIN", null, true, NOW, NOW, Set.of());
        when(currentUserPort.hasPermission("TWO_FACTOR_AUTH_MANAGE")).thenReturn(true);
        when(currentUserPort.getCurrentUserId()).thenReturn(ADMIN_ID);
        when(clockPort.now()).thenReturn(NOW);
        when(roleRepository.findByName("ADMIN")).thenReturn(Optional.of(admin));
        when(roleRepository.save(admin)).thenReturn(admin);

        TwoFactorConfigurationResult result = service.configure(new ConfigureTwoFactorAuthenticationCommand("admin", true));

        assertTrue(result.twoFactorRequired());
        assertTrue(admin.isTwoFactorRequired());
    }

    @Test
    void unsupportedRole_throwsValidation() {
        assertThrows(ValidationException.class,
                () -> service.configure(new ConfigureTwoFactorAuthenticationCommand("RECEPTIONIST", true)));
    }

    @Test
    void unsupportedManagerRole_throwsValidation() {
        assertThrows(ValidationException.class,
                () -> service.configure(new ConfigureTwoFactorAuthenticationCommand("MANAGER", true)));
    }

    @Test
    void unsupportedPharmacistRole_throwsValidation() {
        assertThrows(ValidationException.class,
                () -> service.configure(new ConfigureTwoFactorAuthenticationCommand("PHARMACIST", true)));
    }

    @Test
    void unauthorizedUser_throwsAccessDenied() {
        when(currentUserPort.hasPermission("TWO_FACTOR_AUTH_MANAGE")).thenReturn(false);

        assertThrows(AccessDeniedException.class,
                () -> service.configure(new ConfigureTwoFactorAuthenticationCommand("DOCTOR", true)));
    }

    @Test
    void unknownRole_throwsRoleNotFound() {
        when(currentUserPort.hasPermission("TWO_FACTOR_AUTH_MANAGE")).thenReturn(true);
        when(roleRepository.findByName("DOCTOR")).thenReturn(Optional.empty());

        assertThrows(RoleNotFoundException.class,
                () -> service.configure(new ConfigureTwoFactorAuthenticationCommand("DOCTOR", true)));
    }
}

package com.benhsoan.application.ucservice.role;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.lenient;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.benhsoan.domain.auditlog.AuditLog;
import com.benhsoan.domain.auth.Permission;
import com.benhsoan.domain.auth.Role;
import com.benhsoan.domain.auth.User;
import com.benhsoan.domain.auth.exception.LastAdministratorPermissionException;
import com.benhsoan.domain.shared.exception.ValidationException;
import com.benhsoan.port.dto.command.role.UpdateRolePermissionsCommand;
import com.benhsoan.port.outbound.repository.audit.AuditLogRepository;
import com.benhsoan.port.outbound.repository.auth.PermissionRepository;
import com.benhsoan.port.outbound.repository.auth.RoleRepository;
import com.benhsoan.port.outbound.repository.auth.UserRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;

@ExtendWith(MockitoExtension.class)
class UpdateRolePermissionsServiceTest {
    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");
    private static final UUID ROLE_ID = UUID.randomUUID();
    private static final UUID USER_ID = UUID.randomUUID();
    @Mock RoleRepository roleRepository; @Mock PermissionRepository permissionRepository;
    @Mock UserRepository userRepository; @Mock CurrentUserPort currentUserPort;
    @Mock AuditLogRepository auditLogRepository; @Mock RolePermissionsResultMapper mapper;
    @Captor ArgumentCaptor<AuditLog> auditCaptor;
    @InjectMocks UpdateRolePermissionsService service;
    Role role; User actor;

    @BeforeEach void setUp() {
        role = Role.restore(ROLE_ID, "ADMIN", "Admin", true, NOW, NOW, Set.of(permission("ROLE_READ"), permission("REPORT_EXPORT")));
        actor = User.restore(USER_ID, "admin", "hash", "Admin", "admin@example.com", null, ROLE_ID, true, null, NOW);
        lenient().when(roleRepository.findById(ROLE_ID)).thenReturn(Optional.of(role));
        lenient().when(userRepository.findById(USER_ID)).thenReturn(Optional.of(actor));
        lenient().when(currentUserPort.getCurrentUserId()).thenReturn(USER_ID);
    }

    @Test void updatesPermissionsAndWritesBeforeAfterAudit() {
        List<String> codes = List.of("ROLE_READ", "ROLE_UPDATE", "PERMISSION_READ");
        when(permissionRepository.findAllByCodes(Set.copyOf(codes))).thenReturn(codes.stream().map(this::permission).toList());
        when(userRepository.countActiveByRoleId(ROLE_ID)).thenReturn(2L);
        when(roleRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        service.updateRolePermissions(new UpdateRolePermissionsCommand(ROLE_ID, codes));
        verify(auditLogRepository).save(auditCaptor.capture());
        AuditLog audit = auditCaptor.getValue();
        org.junit.jupiter.api.Assertions.assertEquals(USER_ID, audit.getUserId());
        org.junit.jupiter.api.Assertions.assertEquals(com.benhsoan.domain.auditlog.enums.ActionType.UPDATE, audit.getActionType());
        org.junit.jupiter.api.Assertions.assertEquals(com.benhsoan.domain.auditlog.enums.ResourceType.ROLE, audit.getResourceType());
        org.junit.jupiter.api.Assertions.assertNotNull(audit.getCreatedAt());
        String detail = audit.getDetail();
        org.junit.jupiter.api.Assertions.assertTrue(detail.contains("\"before\""));
        org.junit.jupiter.api.Assertions.assertTrue(detail.contains("\"after\""));
        org.junit.jupiter.api.Assertions.assertTrue(detail.contains("\"added\""));
        org.junit.jupiter.api.Assertions.assertTrue(detail.contains("\"removed\""));
    }

    @Test void rejectsDuplicateOrUnknownOrInactivePermissions() {
        assertThrows(ValidationException.class, () -> service.updateRolePermissions(new UpdateRolePermissionsCommand(ROLE_ID, List.of("ROLE_READ", "ROLE_READ"))));
        when(permissionRepository.findAllByCodes(Set.of("UNKNOWN"))).thenReturn(List.of());
        assertThrows(ValidationException.class, () -> service.updateRolePermissions(new UpdateRolePermissionsCommand(ROLE_ID, List.of("UNKNOWN"))));
        when(permissionRepository.findAllByCodes(Set.of("ROLE_READ"))).thenReturn(List.of(Permission.restore(UUID.randomUUID(), "ROLE_READ", "ROLE READ", "ROLE", null, false, NOW, NOW)));
        assertThrows(ValidationException.class, () -> service.updateRolePermissions(new UpdateRolePermissionsCommand(ROLE_ID, List.of("ROLE_READ"))));
    }

    @Test void blocksOnlyAdministratorRemovingManagementPermissionWithoutSavingOrAuditing() {
        when(permissionRepository.findAllByCodes(Set.of("ROLE_READ", "PERMISSION_READ"))).thenReturn(List.of(permission("ROLE_READ"), permission("PERMISSION_READ")));
        when(userRepository.countActiveByRoleId(ROLE_ID)).thenReturn(1L);
        assertThrows(LastAdministratorPermissionException.class, () -> service.updateRolePermissions(new UpdateRolePermissionsCommand(ROLE_ID, List.of("ROLE_READ", "PERMISSION_READ"))));
        verify(roleRepository, never()).save(any());
        verify(auditLogRepository, never()).save(any());
    }

    private Permission permission(String code) { return Permission.restore(UUID.randomUUID(), code, code, "TEST", null, true, NOW, NOW); }
}

package com.benhsoan.application.ucservice.user;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.benhsoan.application.ucservice.auditlog.AdminOperationAuditService;
import com.benhsoan.domain.auditlog.enums.ActionType;
import com.benhsoan.domain.auditlog.enums.ResourceType;
import com.benhsoan.domain.auth.Role;
import com.benhsoan.domain.auth.User;
import com.benhsoan.port.dto.result.UserResult;
import com.benhsoan.port.outbound.repository.auth.RoleRepository;
import com.benhsoan.port.outbound.repository.auth.UserRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.benhsoan.port.outbound.time.ClockPort;

@ExtendWith(MockitoExtension.class)
class DeactivateUserServiceTest {

    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");

    @Mock private UserRepository userRepository;
    @Mock private RoleRepository roleRepository;
    @Mock private UserResultMapper userResultMapper;
    @Mock private AdminOperationAuditService adminOperationAuditService;
    @Mock private CurrentUserPort currentUserPort;
    @Mock private ClockPort clockPort;

    private DeactivateUserService service;

    @BeforeEach
    void setUp() {
        service = new DeactivateUserService(userRepository, roleRepository, userResultMapper,
                adminOperationAuditService, currentUserPort, clockPort);
    }

    @Test
    void lockAccountRecordsBeforeActiveAndAfterInactive() {
        UUID adminId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID roleId = UUID.randomUUID();
        when(currentUserPort.getCurrentUserId()).thenReturn(adminId);
        when(clockPort.now()).thenReturn(NOW);

        User user = User.restore(userId, "doctor1", "hash", "Doctor One", "d@x.com", null,
                roleId, true, null, NOW);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));
        Role role = mock(Role.class);
        when(role.getName()).thenReturn("DOCTOR");
        when(roleRepository.findById(roleId)).thenReturn(Optional.of(role));

        service.deactivate(userId);

        ArgumentCaptor<Map> before = ArgumentCaptor.forClass(Map.class);
        ArgumentCaptor<Map> after = ArgumentCaptor.forClass(Map.class);
        verify(adminOperationAuditService).record(eq(adminId), eq(ActionType.DEACTIVATE),
                eq(ResourceType.USER), eq(userId), before.capture(), after.capture(), eq(NOW));

        assertEquals(Boolean.TRUE, before.getValue().get("active"));
        assertEquals(Boolean.FALSE, after.getValue().get("active"));
        assertEquals("doctor1", after.getValue().get("username"));
    }

    @Test
    void deactivatingAlreadyInactiveAccountRecordsNoMisleadingAudit() {
        UUID userId = UUID.randomUUID();
        UUID roleId = UUID.randomUUID();

        User user = User.restore(userId, "doctor1", "hash", "Doctor One", "d@x.com", null,
                roleId, false, null, NOW);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));
        Role role = mock(Role.class);
        when(roleRepository.findById(roleId)).thenReturn(Optional.of(role));
        when(userResultMapper.toResult(any(User.class), eq(role))).thenReturn(
                new UserResult(userId, "doctor1", "Doctor One", "d@x.com", null, "DOCTOR", false));

        service.deactivate(userId);

        verify(adminOperationAuditService, never())
                .record(any(), any(), any(), any(), any(), any(), any());
    }
}

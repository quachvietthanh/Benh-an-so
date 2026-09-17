package com.benhsoan.application.ucservice.user;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.benhsoan.application.ucservice.auditlog.AdminOperationAuditService;
import com.benhsoan.domain.auditlog.enums.ActionType;
import com.benhsoan.domain.auditlog.enums.ResourceType;
import com.benhsoan.domain.auth.Role;
import com.benhsoan.domain.auth.User;
import com.benhsoan.port.dto.command.user.UpdateUserCommand;
import com.benhsoan.port.dto.result.UserResult;
import com.benhsoan.port.outbound.repository.auth.RoleRepository;
import com.benhsoan.port.outbound.repository.auth.UserRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.benhsoan.port.outbound.time.ClockPort;

@ExtendWith(MockitoExtension.class)
class UpdateUserServiceTest {

    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");

    @Mock private UserRepository userRepository;
    @Mock private RoleRepository roleRepository;
    @Mock private UserResultMapper userResultMapper;
    @Mock private AdminOperationAuditService adminOperationAuditService;
    @Mock private CurrentUserPort currentUserPort;
    @Mock private ClockPort clockPort;

    private UpdateUserService service;

    @BeforeEach
    void setUp() {
        service = new UpdateUserService(userRepository, roleRepository, userResultMapper,
                adminOperationAuditService, currentUserPort, clockPort);
    }

    @Test
    void updateWithIdenticalValuesSkipsSaveAndAudit() {
        UUID userId = UUID.randomUUID();
        UUID roleId = UUID.randomUUID();

        User user = User.restore(userId, "doctor1", "hash", "Doctor One", "d@x.com",
                "0901234567", roleId, true, null, NOW);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        Role role = mock(Role.class);
        when(role.getId()).thenReturn(roleId);
        when(roleRepository.findById(roleId)).thenReturn(Optional.of(role));
        when(roleRepository.findByName("DOCTOR")).thenReturn(Optional.of(role));
        when(userResultMapper.toResult(any(User.class), eq(role))).thenReturn(
                new UserResult(userId, "doctor1", "Doctor One", "d@x.com", "0901234567", "DOCTOR", true));

        service.update(userId, new UpdateUserCommand(
                "Doctor One", "d@x.com", "0901234567", "DOCTOR"));

        verify(userRepository, never()).save(any());
        verify(adminOperationAuditService, never())
                .record(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void updateWithChangedValuesStillSavesAndRecordsAudit() {
        UUID userId = UUID.randomUUID();
        UUID roleId = UUID.randomUUID();

        User user = User.restore(userId, "doctor1", "hash", "Doctor One", "d@x.com",
                "0901234567", roleId, true, null, NOW);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        Role role = mock(Role.class);
        when(role.getId()).thenReturn(roleId);
        when(role.getName()).thenReturn("DOCTOR");
        when(roleRepository.findById(roleId)).thenReturn(Optional.of(role));
        when(roleRepository.findByName("DOCTOR")).thenReturn(Optional.of(role));
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));
        when(userResultMapper.toResult(any(User.class), eq(role))).thenReturn(
                new UserResult(userId, "doctor1", "Doctor Updated", "d@x.com", "0901234567", "DOCTOR", true));

        service.update(userId, new UpdateUserCommand(
                "Doctor Updated", "d@x.com", "0901234567", "DOCTOR"));

        verify(userRepository).save(any(User.class));
        verify(adminOperationAuditService).record(
                any(), eq(ActionType.UPDATE), eq(ResourceType.USER), any(), any(), any(), any());
    }
}

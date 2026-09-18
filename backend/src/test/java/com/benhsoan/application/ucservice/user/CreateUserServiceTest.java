package com.benhsoan.application.ucservice.user;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
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
import com.benhsoan.port.dto.command.user.CreateUserCommand;
import com.benhsoan.port.dto.result.UserResult;
import com.benhsoan.port.outbound.authSecurity.PasswordEncoderPort;
import com.benhsoan.port.outbound.repository.auth.RoleRepository;
import com.benhsoan.port.outbound.repository.auth.UserRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.benhsoan.port.outbound.time.ClockPort;

@ExtendWith(MockitoExtension.class)
class CreateUserServiceTest {

    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");

    @Mock private UserRepository userRepository;
    @Mock private RoleRepository roleRepository;
    @Mock private PasswordEncoderPort passwordEncoder;
    @Mock private ClockPort clockPort;
    @Mock private UserResultMapper userResultMapper;
    @Mock private AdminOperationAuditService adminOperationAuditService;
    @Mock private CurrentUserPort currentUserPort;

    private CreateUserService service;

    @BeforeEach
    void setUp() {
        service = new CreateUserService(userRepository, roleRepository, passwordEncoder,
                clockPort, userResultMapper, adminOperationAuditService, currentUserPort);
    }

    @Test
    void createUserAuditAfterSnapshotIncludesPhone() {
        UUID adminId = UUID.randomUUID();
        UUID roleId = UUID.randomUUID();
        when(currentUserPort.getCurrentUserId()).thenReturn(adminId);
        when(clockPort.now()).thenReturn(NOW);

        when(userRepository.existsByUsername("doctor1")).thenReturn(false);
        when(userRepository.existsByEmail("d@x.com")).thenReturn(false);
        when(userRepository.existsByPhone("0901234567")).thenReturn(false);

        Role role = mock(Role.class);
        when(role.getId()).thenReturn(roleId);
        when(role.getName()).thenReturn("DOCTOR");
        when(roleRepository.findByName("DOCTOR")).thenReturn(Optional.of(role));

        when(passwordEncoder.encode("secret")).thenReturn("hashed");
        when(userRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(userResultMapper.toResult(any(), eq(role))).thenReturn(
                new UserResult(null, "doctor1", "Doctor One", "d@x.com", "0901234567", "DOCTOR", true));

        service.createUser(new CreateUserCommand(
                "doctor1", "secret", "Doctor One", "d@x.com", "0901234567", "DOCTOR"));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> afterCaptor = ArgumentCaptor.forClass(Map.class);
        verify(adminOperationAuditService).record(
                eq(adminId), eq(ActionType.CREATE), eq(ResourceType.USER), any(),
                isNull(), afterCaptor.capture(), eq(NOW));

        assertEquals("0901234567", afterCaptor.getValue().get("phone"));
    }
}

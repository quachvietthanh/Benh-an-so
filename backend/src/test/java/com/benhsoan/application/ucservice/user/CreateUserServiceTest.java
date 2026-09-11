package com.benhsoan.application.ucservice.user;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.benhsoan.domain.auth.Role;
import com.benhsoan.domain.auth.User;
import com.benhsoan.domain.auth.exception.PhoneAlreadyExistsException;
import com.benhsoan.port.dto.command.user.CreateUserCommand;
import com.benhsoan.port.dto.result.UserResult;
import com.benhsoan.port.outbound.authSecurity.PasswordEncoderPort;
import com.benhsoan.port.outbound.repository.audit.AuditLogRepository;
import com.benhsoan.port.outbound.repository.auth.RoleRepository;
import com.benhsoan.port.outbound.repository.auth.UserRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.benhsoan.port.outbound.time.ClockPort;

@ExtendWith(MockitoExtension.class)
class CreateUserServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private RoleRepository roleRepository;
    @Mock
    private PasswordEncoderPort passwordEncoder;
    @Mock
    private ClockPort clockPort;
    @Mock
    private UserResultMapper userResultMapper;
    @Mock
    private AuditLogRepository auditLogRepository;
    @Mock
    private CurrentUserPort currentUserPort;

    private CreateUserService createUserService;

    @BeforeEach
    void setUp() {
        createUserService = new CreateUserService(
                userRepository,
                roleRepository,
                passwordEncoder,
                clockPort,
                userResultMapper,
                auditLogRepository,
                currentUserPort
        );
    }

    @Test
    @DisplayName("createUser with blank phone should normalize phone to null to prevent unique key collisions")
    void createUserWithBlankPhoneNormalizesToNull() {
        CreateUserCommand command = new CreateUserCommand(
                "doctor_test",
                "Password123@",
                "Dr. Test",
                "doctor_test@benhsoan.com",
                "   ",
                "DOCTOR"
        );

        UUID roleId = UUID.randomUUID();
        Role role = mock(Role.class);
        when(role.getId()).thenReturn(roleId);

        when(userRepository.existsByUsername("doctor_test")).thenReturn(false);
        when(userRepository.existsByEmail("doctor_test@benhsoan.com")).thenReturn(false);
        when(roleRepository.findByName("DOCTOR")).thenReturn(Optional.of(role));
        when(passwordEncoder.encode("Password123@")).thenReturn("hashedPassword");
        when(clockPort.now()).thenReturn(Instant.now());
        when(currentUserPort.getCurrentUserId()).thenReturn(UUID.randomUUID());
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(userResultMapper.toResult(any(User.class), any(Role.class)))
                .thenReturn(new UserResult(UUID.randomUUID(), "doctor_test", "Dr. Test", "doctor_test@benhsoan.com", null, "DOCTOR", true));

        createUserService.createUser(command);

        verify(userRepository, never()).existsByPhone(any());

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertNull(userCaptor.getValue().getPhone(), "Blank phone must be normalized to null");
    }

    @Test
    @DisplayName("createUser with duplicate phone throws PhoneAlreadyExistsException")
    void createUserWithDuplicatePhoneThrowsException() {
        CreateUserCommand command = new CreateUserCommand(
                "doctor_test",
                "Password123@",
                "Dr. Test",
                "doctor_test@benhsoan.com",
                "0912345678",
                "DOCTOR"
        );

        when(userRepository.existsByUsername("doctor_test")).thenReturn(false);
        when(userRepository.existsByEmail("doctor_test@benhsoan.com")).thenReturn(false);
        when(userRepository.existsByPhone("0912345678")).thenReturn(true);

        assertThrows(PhoneAlreadyExistsException.class, () -> createUserService.createUser(command));
        verify(roleRepository, never()).findByName(any());
    }
}

package com.benhsoan.application.ucservice.user;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
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
import com.benhsoan.domain.auth.exception.UserNotFoundException;
import com.benhsoan.port.dto.result.UserResult;
import com.benhsoan.port.outbound.authSecurity.LoginAttemptPort;
import com.benhsoan.port.outbound.repository.auth.RoleRepository;
import com.benhsoan.port.outbound.repository.auth.UserRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.benhsoan.port.outbound.time.ClockPort;

@ExtendWith(MockitoExtension.class)
class UnlockUserServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private RoleRepository roleRepository;
    @Mock
    private UserResultMapper userResultMapper;
    @Mock
    private LoginAttemptPort loginAttemptPort;
    @Mock
    private AdminOperationAuditService adminOperationAuditService;
    @Mock
    private CurrentUserPort currentUserPort;
    @Mock
    private ClockPort clockPort;

    private UnlockUserService unlockUserService;

    @BeforeEach
    void setUp() {
        unlockUserService = new UnlockUserService(
                userRepository,
                roleRepository,
                userResultMapper,
                loginAttemptPort,
                adminOperationAuditService,
                currentUserPort,
                clockPort);
    }

    @Test
    @DisplayName("Admin mở khóa tài khoản thành công: Xóa đếm attempt, ghi Audit Log UNLOCK và trả về UserResult")
    void unlockUser_success_clearsAttemptAndRecordsUnlockAuditLog() {
        UUID userId = UUID.randomUUID();
        UUID adminId = UUID.randomUUID();
        UUID roleId = UUID.randomUUID();

        User user = mock(User.class);
        when(user.getId()).thenReturn(userId);
        when(user.getUsername()).thenReturn("bacsi_an");
        when(user.getPhone()).thenReturn("0901234567");
        when(user.getRoleId()).thenReturn(roleId);

        Role role = mock(Role.class);
        UserResult expectedResult = new UserResult(userId, "bacsi_an", "Bác sĩ An", "an@hospital.vn", "0901234567",
                "DOCTOR", true);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(currentUserPort.getCurrentUserId()).thenReturn(adminId);
        when(roleRepository.findById(roleId)).thenReturn(Optional.of(role));
        when(userResultMapper.toResult(user, role)).thenReturn(expectedResult);
        when(loginAttemptPort.isBlocked("bacsi_an")).thenReturn(false);
        when(loginAttemptPort.isBlocked("0901234567")).thenReturn(true);

        UserResult result = unlockUserService.unlockUser(userId);

        assertNotNull(result);
        assertEquals(expectedResult, result);

        // Xác nhận gọi unlock cho cả username và phone
        verify(loginAttemptPort).unlock("bacsi_an");
        verify(loginAttemptPort).unlock("0901234567");

        // Xác nhận ghi Audit Log với ActionType UNLOCK và trạng thái before/after thực tế
        ArgumentCaptor<UUID> actorCaptor = ArgumentCaptor.forClass(UUID.class);
        ArgumentCaptor<ActionType> actionCaptor = ArgumentCaptor.forClass(ActionType.class);
        ArgumentCaptor<ResourceType> resourceCaptor = ArgumentCaptor.forClass(ResourceType.class);
        ArgumentCaptor<UUID> resourceIdCaptor = ArgumentCaptor.forClass(UUID.class);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> beforeCaptor = ArgumentCaptor.forClass(Map.class);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> afterCaptor = ArgumentCaptor.forClass(Map.class);
        verify(adminOperationAuditService).record(
                actorCaptor.capture(), actionCaptor.capture(), resourceCaptor.capture(),
                resourceIdCaptor.capture(), beforeCaptor.capture(), afterCaptor.capture(), any());

        assertEquals(adminId, actorCaptor.getValue());
        assertEquals(ActionType.UNLOCK, actionCaptor.getValue());
        assertEquals(ResourceType.USER, resourceCaptor.getValue());
        assertEquals(userId, resourceIdCaptor.getValue());
        assertEquals(Boolean.TRUE, beforeCaptor.getValue().get("locked"));
        assertEquals(Boolean.FALSE, afterCaptor.getValue().get("locked"));
    }

    @Test
    @DisplayName("Admin mở khóa tài khoản chưa bị khóa: before.locked phản ánh trạng thái thực tế (false)")
    void unlockUser_notBlocked_recordsActualBeforeStateAsFalse() {
        UUID userId = UUID.randomUUID();
        UUID adminId = UUID.randomUUID();
        UUID roleId = UUID.randomUUID();

        User user = mock(User.class);
        when(user.getId()).thenReturn(userId);
        when(user.getUsername()).thenReturn("bacsi_an");
        when(user.getPhone()).thenReturn(null);
        when(user.getRoleId()).thenReturn(roleId);

        Role role = mock(Role.class);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(currentUserPort.getCurrentUserId()).thenReturn(adminId);
        when(roleRepository.findById(roleId)).thenReturn(Optional.of(role));
        when(userResultMapper.toResult(user, role)).thenReturn(new UserResult(
                userId, "bacsi_an", "Bác sĩ An", "an@hospital.vn", null, "DOCTOR", true));
        when(loginAttemptPort.isBlocked("bacsi_an")).thenReturn(false);

        unlockUserService.unlockUser(userId);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> beforeCaptor = ArgumentCaptor.forClass(Map.class);
        verify(adminOperationAuditService).record(
                any(), any(), any(), any(), beforeCaptor.capture(), any(), any());
        assertEquals(Boolean.FALSE, beforeCaptor.getValue().get("locked"));
    }

    @Test
    @DisplayName("Không tìm thấy user khi mở khóa -> Ném UserNotFoundException")
    void unlockUser_notFound_throwsUserNotFoundException() {
        UUID userId = UUID.randomUUID();
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> unlockUserService.unlockUser(userId));
    }
}

package com.benhsoan.application.ucservice.user;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
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
import com.benhsoan.domain.auth.exception.UserNotFoundException;
import com.benhsoan.port.dto.result.UserResult;
import com.benhsoan.port.outbound.authSecurity.LoginAttemptPort;
import com.benhsoan.port.outbound.repository.audit.AuditLogRepository;
import com.benhsoan.port.outbound.repository.auth.RoleRepository;
import com.benhsoan.port.outbound.repository.auth.UserRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;

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
    private AuditLogRepository auditLogRepository;
    @Mock
    private CurrentUserPort currentUserPort;

    private UnlockUserService unlockUserService;

    @BeforeEach
    void setUp() {
        unlockUserService = new UnlockUserService(
                userRepository,
                roleRepository,
                userResultMapper,
                loginAttemptPort,
                auditLogRepository,
                currentUserPort);
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

        UserResult result = unlockUserService.unlockUser(userId);

        assertNotNull(result);
        assertEquals(expectedResult, result);

        // Xác nhận gọi unlock cho cả username và phone
        verify(loginAttemptPort).unlock("bacsi_an");
        verify(loginAttemptPort).unlock("0901234567");

        // Xác nhận ghi Audit Log với ActionType UNLOCK
        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        AuditLog savedLog = captor.getValue();

        assertEquals(adminId, savedLog.getUserId());
        assertEquals(ActionType.UNLOCK, savedLog.getActionType());
        assertEquals(ResourceType.USER, savedLog.getResourceType());
        assertEquals(userId, savedLog.getResourceId());
    }

    @Test
    @DisplayName("Không tìm thấy user khi mở khóa -> Ném UserNotFoundException")
    void unlockUser_notFound_throwsUserNotFoundException() {
        UUID userId = UUID.randomUUID();
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> unlockUserService.unlockUser(userId));
    }
}

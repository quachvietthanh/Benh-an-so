package com.benhsoan.application.ucservice.user;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import com.benhsoan.domain.auditlog.AuditLog;
import com.benhsoan.domain.auditlog.enums.ActionType;
import com.benhsoan.domain.auditlog.enums.ResourceType;
import com.benhsoan.domain.auth.User;
import com.benhsoan.domain.auth.exception.UserNotFoundException;
import com.benhsoan.port.dto.result.LoginAuditLogResult;
import com.benhsoan.port.outbound.repository.audit.AuditLogRepository;
import com.benhsoan.port.outbound.repository.auth.UserRepository;

@ExtendWith(MockitoExtension.class)
class GetLoginAuditLogsServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private AuditLogRepository auditLogRepository;

    private GetLoginAuditLogsService service;

    @BeforeEach
    void setUp() {
        service = new GetLoginAuditLogsService(userRepository, auditLogRepository);
    }

    @Test
    @DisplayName("TC-04: Tra cứu nhật ký đăng nhập thành công có phân trang")
    void getLoginAuditLogs_success_returnsPagedResults() {
        UUID userId = UUID.randomUUID();
        User user = mock(User.class);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        Pageable pageable = PageRequest.of(0, 10);
        AuditLog lockLog = AuditLog.create(userId, ActionType.LOCK, ResourceType.USER, userId, "{\"failedAttempts\":5}", null, Instant.now());
        Page<AuditLog> pagedLogs = new PageImpl<>(List.of(lockLog), pageable, 1);

        when(auditLogRepository.findLoginAuditLogs(userId, pageable)).thenReturn(pagedLogs);

        Page<LoginAuditLogResult> result = service.getLoginAuditLogs(userId, pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        LoginAuditLogResult item = result.getContent().get(0);
        assertEquals(ActionType.LOCK, item.actionType());
        assertEquals(ResourceType.USER, item.resourceType());
        assertEquals(userId, item.resourceId());
    }

    @Test
    @DisplayName("Tra cứu khi user không tồn tại -> Ném UserNotFoundException")
    void getLoginAuditLogs_userNotFound_throwsUserNotFoundException() {
        UUID userId = UUID.randomUUID();
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> service.getLoginAuditLogs(userId, PageRequest.of(0, 10)));
    }
}

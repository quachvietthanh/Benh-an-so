package com.benhsoan.application.ucservice.billing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.domain.auditlog.AuditLog;
import com.benhsoan.domain.auditlog.enums.ActionType;
import com.benhsoan.domain.auditlog.enums.ResourceType;
import com.benhsoan.port.outbound.repository.audit.AuditLogRepository;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.mockito.Spy;

@ExtendWith(MockitoExtension.class)
@DisplayName("CashierShiftAuthorizationAuditService Unit Tests")
class CashierShiftAuthorizationAuditServiceTest {

    @Mock
    private AuditLogRepository auditLogRepository;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private CashierShiftAuthorizationAuditService auditService;

    @Test
    @DisplayName("P1: Ghi nhận ACCESS_DENIED audit log cho thao tác confirm bị từ chối với đầy đủ ngữ cảnh")
    void shouldRecordConfirmAccessDeniedWithCorrectContext() {
        UUID actorId = UUID.randomUUID();
        UUID shiftId = UUID.randomUUID();
        String reason = "User lacks MANAGER role";

        auditService.recordConfirmAccessDenied(actorId, shiftId, reason);

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());

        AuditLog savedLog = captor.getValue();
        assertNotNull(savedLog);
        assertEquals(actorId, savedLog.getUserId());
        assertEquals(ActionType.ACCESS_DENIED, savedLog.getActionType());
        assertEquals(ResourceType.CASHIER_SHIFT, savedLog.getResourceType());
        assertEquals(shiftId, savedLog.getResourceId());
        assertNotNull(savedLog.getDetail());
    }

    @Test
    @DisplayName("P1: Ghi nhận ACCESS_DENIED audit log cho thao tác close bị từ chối")
    void shouldRecordCloseAccessDeniedWithCorrectContext() {
        UUID actorId = UUID.randomUUID();
        String reason = "User lacks RECEPTIONIST role or CASHIER_SHIFT_CREATE permission";

        auditService.recordCloseAccessDenied(actorId, reason);

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());

        AuditLog savedLog = captor.getValue();
        assertNotNull(savedLog);
        assertEquals(actorId, savedLog.getUserId());
        assertEquals(ActionType.ACCESS_DENIED, savedLog.getActionType());
        assertEquals(ResourceType.CASHIER_SHIFT, savedLog.getResourceType());
        assertNotNull(savedLog.getDetail());
    }

    @Test
    @DisplayName("P1: Các phương thức audit có cấu hình Propagation.REQUIRES_NEW để độc lập với transaction nghiệp vụ")
    void shouldHaveRequiresNewTransactionAnnotation() throws NoSuchMethodException {
        Transactional confirmTx = CashierShiftAuthorizationAuditService.class
                .getMethod("recordConfirmAccessDenied", UUID.class, UUID.class, String.class)
                .getAnnotation(Transactional.class);
        assertNotNull(confirmTx);
        assertEquals(Propagation.REQUIRES_NEW, confirmTx.propagation());

        Transactional closeTx = CashierShiftAuthorizationAuditService.class
                .getMethod("recordCloseAccessDenied", UUID.class, String.class)
                .getAnnotation(Transactional.class);
        assertNotNull(closeTx);
        assertEquals(Propagation.REQUIRES_NEW, closeTx.propagation());
    }
}

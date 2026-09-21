package com.benhsoan.application.ucservice.inventory;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

import com.benhsoan.domain.auditlog.enums.ActionType;
import com.benhsoan.domain.auditlog.enums.ResourceType;
import com.benhsoan.port.outbound.repository.audit.AuditLogRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.benhsoan.port.outbound.time.ClockPort;

class InventoryReportAuthorizerTest {

    private static final Instant NOW = Instant.parse("2026-09-21T10:00:00Z");
    private static final UUID USER_ID = UUID.randomUUID();

    private final CurrentUserPort currentUserPort = mock(CurrentUserPort.class);
    private final AuditLogRepository auditLogRepository = mock(AuditLogRepository.class);
    private final ClockPort clockPort = mock(ClockPort.class);

    private InventoryReportAuthorizer authorizer;

    @BeforeEach
    void setUp() {
        when(clockPort.now()).thenReturn(NOW);
        when(currentUserPort.getCurrentUserId()).thenReturn(USER_ID);

        authorizer = new InventoryReportAuthorizer(
                currentUserPort,
                auditLogRepository,
                clockPort
        );
    }

    @Test
    void allowsPharmacist() {
        when(currentUserPort.hasRole("PHARMACIST")).thenReturn(true);

        assertDoesNotThrow(() -> authorizer.requireReportAccess());
        verify(auditLogRepository, never()).save(any());
    }

    @Test
    void allowsManager() {
        when(currentUserPort.hasRole("PHARMACIST")).thenReturn(false);
        when(currentUserPort.hasRole("MANAGER")).thenReturn(true);

        assertDoesNotThrow(() -> authorizer.requireReportAccess());
        verify(auditLogRepository, never()).save(any());
    }

    @Test
    void allowsAdmin() {
        when(currentUserPort.hasRole("PHARMACIST")).thenReturn(false);
        when(currentUserPort.hasRole("MANAGER")).thenReturn(false);
        when(currentUserPort.hasRole("ADMIN")).thenReturn(true);

        assertDoesNotThrow(() -> authorizer.requireReportAccess());
        verify(auditLogRepository, never()).save(any());
    }

    @Test
    void deniesDoctorAndRecordsAuditLog() {
        when(currentUserPort.hasRole("PHARMACIST")).thenReturn(false);
        when(currentUserPort.hasRole("MANAGER")).thenReturn(false);
        when(currentUserPort.hasRole("ADMIN")).thenReturn(false);
        when(currentUserPort.getCurrentUserRoles()).thenReturn(Set.of("DOCTOR"));

        AccessDeniedException ex = assertThrows(AccessDeniedException.class, () -> authorizer.requireReportAccess());
        assertEquals("Chỉ dược sĩ, quản lý phòng khám hoặc quản trị viên mới có quyền truy cập báo cáo xuất nhập tồn kho.", ex.getMessage());

        verify(auditLogRepository).save(argThat(audit ->
                audit.getUserId().equals(USER_ID)
                        && audit.getActionType() == ActionType.ACCESS_DENIED
                        && audit.getResourceType() == ResourceType.OPERATIONAL_REPORT
                        && audit.getDetail().contains("DOCTOR")
        ));
    }

    @Test
    void deniesReceptionist() {
        when(currentUserPort.hasRole("PHARMACIST")).thenReturn(false);
        when(currentUserPort.hasRole("MANAGER")).thenReturn(false);
        when(currentUserPort.hasRole("ADMIN")).thenReturn(false);
        when(currentUserPort.getCurrentUserRoles()).thenReturn(Set.of("RECEPTIONIST"));

        assertThrows(AccessDeniedException.class, () -> authorizer.requireReportAccess());

        verify(auditLogRepository).save(argThat(audit ->
                audit.getActionType() == ActionType.ACCESS_DENIED
                        && audit.getDetail().contains("RECEPTIONIST")
        ));
    }
}

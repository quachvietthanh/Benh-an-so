package com.benhsoan.application.ucservice.inventory;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.benhsoan.port.outbound.time.ClockPort;

class InventoryReportAuthorizerTest {

    private static final Instant NOW = Instant.parse("2026-09-21T10:00:00Z");
    private static final UUID USER_ID = UUID.randomUUID();

    private final CurrentUserPort currentUserPort = mock(CurrentUserPort.class);
    private final InventoryReportAccessDeniedAuditWriter auditWriter = mock(InventoryReportAccessDeniedAuditWriter.class);
    private final ClockPort clockPort = mock(ClockPort.class);

    private InventoryReportAuthorizer authorizer;

    @BeforeEach
    void setUp() {
        when(clockPort.now()).thenReturn(NOW);
        when(currentUserPort.getCurrentUserId()).thenReturn(USER_ID);

        authorizer = new InventoryReportAuthorizer(
                currentUserPort,
                auditWriter,
                clockPort
        );
    }

    @Test
    void allowsPharmacist() {
        when(currentUserPort.hasRole("PHARMACIST")).thenReturn(true);

        assertDoesNotThrow(() -> authorizer.requireReportAccess());
        verifyNoInteractions(auditWriter);
    }

    @Test
    void allowsManager() {
        when(currentUserPort.hasRole("PHARMACIST")).thenReturn(false);
        when(currentUserPort.hasRole("MANAGER")).thenReturn(true);

        assertDoesNotThrow(() -> authorizer.requireReportAccess());
        verifyNoInteractions(auditWriter);
    }

    @Test
    void allowsAdmin() {
        when(currentUserPort.hasRole("PHARMACIST")).thenReturn(false);
        when(currentUserPort.hasRole("MANAGER")).thenReturn(false);
        when(currentUserPort.hasRole("ADMIN")).thenReturn(true);

        assertDoesNotThrow(() -> authorizer.requireReportAccess());
        verifyNoInteractions(auditWriter);
    }

    @Test
    void deniesDoctorAndRecordsAuditLog() {
        when(currentUserPort.hasRole("PHARMACIST")).thenReturn(false);
        when(currentUserPort.hasRole("MANAGER")).thenReturn(false);
        when(currentUserPort.hasRole("ADMIN")).thenReturn(false);
        when(currentUserPort.getCurrentUserRoles()).thenReturn(Set.of("DOCTOR"));

        AccessDeniedException ex = assertThrows(AccessDeniedException.class, () -> authorizer.requireReportAccess());
        assertEquals("Chỉ dược sĩ, quản lý phòng khám hoặc quản trị viên mới có quyền truy cập báo cáo xuất nhập tồn kho.", ex.getMessage());

        verify(auditWriter).writeAccessDenied(
                eq(USER_ID),
                eq(Set.of("DOCTOR")),
                eq("Chỉ dược sĩ, quản lý phòng khám hoặc quản trị viên mới có quyền truy cập báo cáo xuất nhập tồn kho."),
                eq(NOW)
        );
    }

    @Test
    void deniesReceptionist() {
        when(currentUserPort.hasRole("PHARMACIST")).thenReturn(false);
        when(currentUserPort.hasRole("MANAGER")).thenReturn(false);
        when(currentUserPort.hasRole("ADMIN")).thenReturn(false);
        when(currentUserPort.getCurrentUserRoles()).thenReturn(Set.of("RECEPTIONIST"));

        assertThrows(AccessDeniedException.class, () -> authorizer.requireReportAccess());

        verify(auditWriter).writeAccessDenied(
                eq(USER_ID),
                eq(Set.of("RECEPTIONIST")),
                anyString(),
                eq(NOW)
        );
    }
}

package com.benhsoan.application.ucservice.reporting;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.benhsoan.domain.auditlog.AuditLog;
import com.benhsoan.domain.auditlog.enums.ActionType;
import com.benhsoan.domain.auditlog.enums.ResourceType;
import com.benhsoan.domain.reporting.enums.ReportType;
import com.benhsoan.port.outbound.repository.audit.AuditLogRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.benhsoan.port.outbound.time.ClockPort;

class OperationalReportAuditServiceTest {

    @Test
    void writesSelectedReportTypeToExportAuditLog() {
        AuditLogRepository auditLogRepository = mock(AuditLogRepository.class);
        CurrentUserPort currentUserPort = mock(CurrentUserPort.class);
        ClockPort clockPort = mock(ClockPort.class);
        UUID actorId = UUID.randomUUID();
        Instant exportedAt = Instant.parse("2026-08-13T02:15:30Z");

        when(currentUserPort.getCurrentUserId()).thenReturn(actorId);
        when(currentUserPort.getCurrentUserRoles()).thenReturn(Set.of("MANAGER"));
        when(clockPort.now()).thenReturn(exportedAt);

        OperationalReportAuditService service = new OperationalReportAuditService(
                auditLogRepository,
                currentUserPort,
                clockPort
        );

        service.logExport(ReportType.REVENUE_REPORT, LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 3));

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());

        AuditLog auditLog = captor.getValue();
        assertEquals(actorId, auditLog.getUserId());
        assertEquals(ActionType.EXPORT, auditLog.getActionType());
        assertEquals(ResourceType.OPERATIONAL_REPORT, auditLog.getResourceType());
        assertNull(auditLog.getResourceId());
        assertEquals(exportedAt, auditLog.getCreatedAt());
        assertTrue(auditLog.getDetail().contains("\"reportType\":\"REVENUE_REPORT\""));
        assertTrue(auditLog.getDetail().contains("\"role\":\"MANAGER\""));
        assertTrue(auditLog.getDetail().contains("\"from\":\"2026-08-01\""));
        assertTrue(auditLog.getDetail().contains("\"to\":\"2026-08-03\""));
        assertTrue(auditLog.getDetail().contains("\"exportedAt\":\"2026-08-13T02:15:30Z\""));
    }

    @Test
    void prefersManagerRoleInAuditWhenUserHasMultipleRoles() {
        AuditLogRepository auditLogRepository = mock(AuditLogRepository.class);
        CurrentUserPort currentUserPort = mock(CurrentUserPort.class);
        ClockPort clockPort = mock(ClockPort.class);

        when(currentUserPort.getCurrentUserId()).thenReturn(UUID.randomUUID());
        when(currentUserPort.getCurrentUserRoles()).thenReturn(Set.of("ADMIN", "MANAGER"));
        when(clockPort.now()).thenReturn(Instant.parse("2026-08-13T02:15:30Z"));

        OperationalReportAuditService service = new OperationalReportAuditService(
                auditLogRepository,
                currentUserPort,
                clockPort
        );

        service.logExport(ReportType.OPERATIONAL_REPORT, LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 3));

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());

        assertTrue(captor.getValue().getDetail().contains("\"role\":\"MANAGER\""));
    }
}

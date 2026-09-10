package com.benhsoan.application.ucservice.reporting;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.benhsoan.domain.auditlog.AuditLog;
import com.benhsoan.domain.auditlog.enums.ActionType;
import com.benhsoan.domain.auditlog.enums.ResourceType;
import com.benhsoan.domain.auth.User;
import com.benhsoan.domain.reporting.exception.AccessLogReportDataEmptyException;
import com.benhsoan.port.dto.result.AccessLogAccountCountResult;
import com.benhsoan.port.dto.result.AccessLogReportExportResult;
import com.benhsoan.port.outbound.repository.audit.AuditLogRepository;
import com.benhsoan.port.outbound.repository.auth.UserRepository;
import com.benhsoan.port.outbound.repository.medicalrecord.MedicalRecordAccessLogRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.benhsoan.port.outbound.time.ClockPort;
import com.fasterxml.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class ExportAccessLogReportServiceTest {

    private static final ZoneId CLINIC_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final UUID ACTOR_A = UUID.randomUUID();
    private static final UUID ACTOR_B = UUID.randomUUID();
    private static final UUID ADMIN_ID = UUID.randomUUID();
    private static final Instant NOW = Instant.parse("2026-09-10T03:00:00Z");

    @Mock private MedicalRecordAccessLogRepository accessLogRepository;
    @Mock private UserRepository userRepository;
    @Mock private AuditLogRepository auditLogRepository;
    @Mock private CurrentUserPort currentUserPort;
    @Mock private ClockPort clockPort;

    private ExportAccessLogReportService service;

    @BeforeEach
    void setUp() {
        service = new ExportAccessLogReportService(
                accessLogRepository,
                userRepository,
                auditLogRepository,
                currentUserPort,
                clockPort,
                new ObjectMapper());
    }

    @Test
    void exportsReportAggregatingAccessCountsByAccountAndAuditsOnce() {
        LocalDate from = LocalDate.parse("2026-09-01");
        LocalDate to = LocalDate.parse("2026-09-30");

        when(accessLogRepository.countAccessByAccountBetween(any(Instant.class), any(Instant.class)))
                .thenReturn(List.of(
                        new AccessLogAccountCountResult(ACTOR_A, 3L),
                        new AccessLogAccountCountResult(ACTOR_B, 1L)));
        when(userRepository.findAllById(anyList()))
                .thenReturn(List.of(
                        user(ACTOR_A, "doctor1", "Dr. A"),
                        user(ACTOR_B, "doctor2", "Dr. B")));
        when(clockPort.now()).thenReturn(NOW);
        when(currentUserPort.getCurrentUserId()).thenReturn(ADMIN_ID);

        AccessLogReportExportResult result = service.export(from, to);

        String csv = new String(result.content(), StandardCharsets.UTF_8);
        assertTrue(csv.startsWith("\uFEFF"), "CSV must carry a UTF-8 BOM for Excel compatibility");
        assertTrue(csv.contains("MEDICAL RECORD ACCESS LOG REPORT"));
        assertTrue(csv.contains("Account,Full Name,Access Count"));
        assertTrue(csv.contains("doctor1,Dr. A,3"));
        assertTrue(csv.contains("doctor2,Dr. B,1"));

        ArgumentCaptor<AuditLog> auditCaptor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(auditCaptor.capture());
        AuditLog audit = auditCaptor.getValue();
        assertEquals(ADMIN_ID, audit.getUserId());
        assertEquals(ActionType.EXPORT, audit.getActionType());
        assertEquals(ResourceType.ACCESS_LOG_REPORT, audit.getResourceType());
        assertTrue(audit.getDetail().contains("ACCESS_LOG_REPORT"));
        assertTrue(audit.getDetail().contains("2026-09-01"));
        assertTrue(audit.getDetail().contains("2026-09-30"));
    }

    @Test
    void throwsWhenNoAccessLogsInPeriodAndDoesNotAudit() {
        when(accessLogRepository.countAccessByAccountBetween(any(Instant.class), any(Instant.class)))
                .thenReturn(List.of());

        assertThrows(AccessLogReportDataEmptyException.class,
                () -> service.export(LocalDate.parse("2026-09-01"), LocalDate.parse("2026-09-30")));

        verify(auditLogRepository, never()).save(any());
    }

    @Test
    void resolvesDeletedAccountsByUserId() {
        when(accessLogRepository.countAccessByAccountBetween(any(Instant.class), any(Instant.class)))
                .thenReturn(List.of(new AccessLogAccountCountResult(ACTOR_A, 2L)));
        when(userRepository.findAllById(anyList())).thenReturn(List.of());
        when(clockPort.now()).thenReturn(NOW);
        when(currentUserPort.getCurrentUserId()).thenReturn(ADMIN_ID);

        AccessLogReportExportResult result = service.export(
                LocalDate.parse("2026-09-01"), LocalDate.parse("2026-09-30"));

        String csv = new String(result.content(), StandardCharsets.UTF_8);
        assertTrue(csv.contains(ACTOR_A.toString() + ",,2"), "Deleted account must fall back to its raw userId.");
    }

    @Test
    void usesFullDayTimezoneBoundariesForSameDayRange() {
        LocalDate from = LocalDate.parse("2026-09-10");
        LocalDate to = LocalDate.parse("2026-09-10");

        when(accessLogRepository.countAccessByAccountBetween(any(Instant.class), any(Instant.class)))
                .thenReturn(List.of(new AccessLogAccountCountResult(ACTOR_A, 1L)));
        when(userRepository.findAllById(anyList())).thenReturn(List.of(user(ACTOR_A, "doctor1", "Dr. A")));
        when(clockPort.now()).thenReturn(NOW);
        when(currentUserPort.getCurrentUserId()).thenReturn(ADMIN_ID);

        service.export(from, to);

        ArgumentCaptor<Instant> fromCaptor = ArgumentCaptor.forClass(Instant.class);
        ArgumentCaptor<Instant> toCaptor = ArgumentCaptor.forClass(Instant.class);
        verify(accessLogRepository).countAccessByAccountBetween(fromCaptor.capture(), toCaptor.capture());

        Instant expectedFrom = from.atStartOfDay(CLINIC_ZONE).toInstant();
        Instant expectedTo = to.plusDays(1).atStartOfDay(CLINIC_ZONE).toInstant();
        assertEquals(expectedFrom, fromCaptor.getValue());
        assertEquals(expectedTo, toCaptor.getValue());
    }

    private User user(UUID id, String username, String fullName) {
        return User.restore(id, username, "$2a$10$hash", fullName, username + "@benhsoan.com", null,
                UUID.randomUUID(), true, null, Instant.parse("2026-01-01T00:00:00Z"));
    }
}

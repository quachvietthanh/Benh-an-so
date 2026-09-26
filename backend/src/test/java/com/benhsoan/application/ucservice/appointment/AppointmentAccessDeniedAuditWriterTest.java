package com.benhsoan.application.ucservice.appointment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.benhsoan.domain.auditlog.AuditLog;
import com.benhsoan.domain.auditlog.enums.ActionType;
import com.benhsoan.domain.auditlog.enums.ResourceType;
import com.benhsoan.port.outbound.repository.audit.AuditLogRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class AppointmentAccessDeniedAuditWriterTest {

    @Mock
    private AuditLogRepository auditLogRepository;

    @Captor
    private ArgumentCaptor<AuditLog> auditLogCaptor;

    private AppointmentAccessDeniedAuditWriter auditWriter;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        auditWriter = new AppointmentAccessDeniedAuditWriter(auditLogRepository, objectMapper);
    }

    @Test
    void writesAccessDeniedAuditEntryCorrectly() {
        UUID actorId = UUID.randomUUID();
        UUID appointmentId = UUID.randomUUID();
        Instant deniedAt = Instant.parse("2026-09-14T08:00:00Z");
        String reason = "User lacks RECEPTIONIST or ADMIN role";

        auditWriter.writeRescheduleDenied(actorId, appointmentId, deniedAt, reason);

        verify(auditLogRepository).save(auditLogCaptor.capture());
        AuditLog saved = auditLogCaptor.getValue();

        assertNotNull(saved);
        assertEquals(actorId, saved.getUserId());
        assertEquals(ActionType.ACCESS_DENIED, saved.getActionType());
        assertEquals(ResourceType.APPOINTMENT, saved.getResourceType());
        assertEquals(appointmentId, saved.getResourceId());
        assertTrue(saved.getDetail().contains("User lacks RECEPTIONIST or ADMIN role"));
    }

    @Test
    void writesConfirmDeniedAuditEntryCorrectly_Finding4() {
        UUID actorId = UUID.randomUUID();
        UUID appointmentId = UUID.randomUUID();
        Instant deniedAt = Instant.parse("2026-09-14T08:00:00Z");
        String reason = "User lacks RECEPTIONIST or ADMIN role to confirm appointment";

        auditWriter.writeConfirmDenied(actorId, appointmentId, deniedAt, reason);

        verify(auditLogRepository).save(auditLogCaptor.capture());
        AuditLog saved = auditLogCaptor.getValue();

        assertNotNull(saved);
        assertEquals(actorId, saved.getUserId());
        assertEquals(ActionType.ACCESS_DENIED, saved.getActionType());
        assertEquals(ResourceType.APPOINTMENT, saved.getResourceType());
        assertEquals(appointmentId, saved.getResourceId());
        assertTrue(saved.getDetail().contains("CONFIRM"));
        assertTrue(saved.getDetail().contains("User lacks RECEPTIONIST or ADMIN role to confirm appointment"));
        assertTrue(saved.getDetail().contains("2026-09-14T08:00:00Z"));
    }
}

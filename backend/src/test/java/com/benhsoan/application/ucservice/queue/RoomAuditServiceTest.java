package com.benhsoan.application.ucservice.queue;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.benhsoan.domain.auditlog.AuditLog;
import com.benhsoan.domain.auditlog.enums.ActionType;
import com.benhsoan.domain.auditlog.enums.ResourceType;
import com.benhsoan.domain.queue.Room;
import com.benhsoan.port.outbound.repository.audit.AuditLogRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.fasterxml.jackson.databind.ObjectMapper;

class RoomAuditServiceTest {

    @Test
    void recordsSafeRoomAuditDetailWithNullIpAddress() throws Exception {
        AuditLogRepository auditLogRepository = mock(AuditLogRepository.class);
        CurrentUserPort currentUserPort = mock(CurrentUserPort.class);
        UUID actorId = UUID.randomUUID();
        when(currentUserPort.getCurrentUserId()).thenReturn(actorId);
        ObjectMapper objectMapper = new ObjectMapper();
        RoomAuditService service = new RoomAuditService(auditLogRepository, currentUserPort, objectMapper);
        Room room = Room.create("P103", "Phong \"VIP\"", Instant.parse("2026-08-02T02:00:00Z"));

        service.record(ActionType.CREATE, room);

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        AuditLog auditLog = captor.getValue();
        assertEquals(actorId, auditLog.getUserId());
        assertEquals(ActionType.CREATE, auditLog.getActionType());
        assertEquals(ResourceType.ROOM, auditLog.getResourceType());
        assertEquals(room.getId(), auditLog.getResourceId());
        assertNull(auditLog.getIpAddress());
        assertEquals("Phong \"VIP\"", objectMapper.readTree(auditLog.getDetail()).get("name").asText());
        assertTrue(auditLog.getDetail().contains("\"status\":\"ACTIVE\""));
    }
}

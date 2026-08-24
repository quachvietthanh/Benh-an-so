package com.benhsoan.application.ucservice.medicalrecord;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.benhsoan.domain.auditlog.AuditLog;
import com.benhsoan.domain.auditlog.enums.ActionType;
import com.benhsoan.domain.auditlog.enums.ResourceType;
import com.benhsoan.port.outbound.repository.audit.AuditLogRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class MedicalRecordAmendmentAuditWriterTest {

    private static final Instant NOW = Instant.parse("2026-08-20T02:00:00Z");

    @Mock
    private AuditLogRepository auditLogRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void writeAmendedSerializesValidJsonDetail() throws Exception {
        UUID actorId = UUID.randomUUID();
        UUID medicalRecordId = UUID.randomUUID();
        MedicalRecordAmendmentAuditWriter writer = new MedicalRecordAmendmentAuditWriter(auditLogRepository, objectMapper);

        writer.writeAmended(actorId, medicalRecordId, "Clarification", NOW);

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        AuditLog log = captor.getValue();
        assertEquals(ActionType.UPDATE, log.getActionType());
        assertEquals(ResourceType.MEDICAL_RECORD, log.getResourceType());

        com.fasterxml.jackson.databind.JsonNode node = objectMapper.readTree(log.getDetail());
        assertNotNull(node);
        assertEquals("AMEND", node.get("action").asText());
        assertEquals(actorId.toString(), node.get("amendedBy").asText());
        assertEquals("Clarification", node.get("reason").asText());
        assertEquals(NOW.toString(), node.get("amendedAt").asText());
    }

    @Test
    void writeDeniedSerializesValidJsonDetail() throws Exception {
        UUID actorId = UUID.randomUUID();
        UUID medicalRecordId = UUID.randomUUID();
        MedicalRecordAmendmentAuditWriter writer = new MedicalRecordAmendmentAuditWriter(auditLogRepository, objectMapper);

        writer.writeDenied(actorId, medicalRecordId, "not the responsible doctor", NOW);

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        AuditLog log = captor.getValue();
        assertEquals(ActionType.ACCESS_DENIED, log.getActionType());

        com.fasterxml.jackson.databind.JsonNode node = objectMapper.readTree(log.getDetail());
        assertNotNull(node);
        assertEquals("ACCESS_DENIED", node.get("action").asText());
        assertEquals("not the responsible doctor", node.get("reason").asText());
        assertEquals(NOW.toString(), node.get("deniedAt").asText());
    }
}

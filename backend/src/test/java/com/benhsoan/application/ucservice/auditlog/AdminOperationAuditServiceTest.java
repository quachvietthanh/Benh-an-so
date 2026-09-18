package com.benhsoan.application.ucservice.auditlog;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
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
import com.benhsoan.port.outbound.repository.audit.AuditLogRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class AdminOperationAuditServiceTest {

    @Mock
    private AuditLogRepository auditLogRepository;

    private AdminOperationAuditService service;

    @BeforeEach
    void setUp() {
        service = new AdminOperationAuditService(auditLogRepository);
    }

    @Test
    void recordsBeforeAfterActorAndTimestampAsJson() throws Exception {
        UUID actorId = UUID.randomUUID();
        UUID resourceId = UUID.randomUUID();
        Instant at = Instant.parse("2026-01-01T00:00:00Z");

        service.record(actorId, ActionType.UPDATE, ResourceType.SERVICE_PRICE, resourceId,
                Map.of("price", new BigDecimal("95000.00")),
                Map.of("price", new BigDecimal("120000.00")), at);

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        AuditLog log = captor.getValue();

        assertEquals(actorId, log.getUserId());
        assertEquals(ActionType.UPDATE, log.getActionType());
        assertEquals(ResourceType.SERVICE_PRICE, log.getResourceType());
        assertEquals(resourceId, log.getResourceId());
        assertEquals(at, log.getCreatedAt());

        var node = new ObjectMapper().readTree(log.getDetail());
        assertEquals(0, new BigDecimal("95000.00").compareTo(node.get("before").get("price").decimalValue()));
        assertEquals(0, new BigDecimal("120000.00").compareTo(node.get("after").get("price").decimalValue()));
    }

    @Test
    void createActionRecordsNullBefore() throws Exception {
        UUID actorId = UUID.randomUUID();
        UUID resourceId = UUID.randomUUID();

        service.record(actorId, ActionType.CREATE, ResourceType.MEDICINE, resourceId,
                null,
                Map.of("medicineName", "Paracetamol"),
                Instant.parse("2026-01-01T00:00:00Z"));

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        var node = new ObjectMapper().readTree(captor.getValue().getDetail());
        assertEquals(true, node.get("before").isNull());
        assertEquals("Paracetamol", node.get("after").get("medicineName").asText());
    }
}

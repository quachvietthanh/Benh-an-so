package com.benhsoan.application.ucservice.billing;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.benhsoan.domain.auditlog.AuditLog;
import com.benhsoan.domain.auditlog.enums.ActionType;
import com.benhsoan.domain.auditlog.enums.ResourceType;
import com.benhsoan.port.outbound.repository.audit.AuditLogRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

@DisplayName("BillingAccessDeniedAuditWriter Tests")
class BillingAccessDeniedAuditWriterTest {

    @Test
    @DisplayName("writePaymentDenied should persist ACCESS_DENIED audit log with visitId and actorId")
    void writePaymentDeniedPersistsAuditLog() {
        AuditLogRepository auditLogRepository = mock(AuditLogRepository.class);
        ObjectMapper objectMapper = new ObjectMapper();
        BillingAccessDeniedAuditWriter writer = new BillingAccessDeniedAuditWriter(auditLogRepository, objectMapper);

        UUID actorId = UUID.randomUUID();
        UUID visitId = UUID.randomUUID();
        Instant deniedAt = Instant.parse("2026-08-11T04:00:00Z");

        writer.writePaymentDenied(actorId, visitId, deniedAt, "User lacks RECEPTIONIST role");

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());

        AuditLog saved = captor.getValue();
        assertEquals(actorId, saved.getUserId());
        assertEquals(ActionType.ACCESS_DENIED, saved.getActionType());
        assertEquals(ResourceType.PAYMENT, saved.getResourceType());
        assertEquals(visitId, saved.getResourceId());
        assertEquals(deniedAt, saved.getCreatedAt());
        assertNotNull(saved.getDetail());
        assertTrue(saved.getDetail().contains("User lacks RECEPTIONIST role"));
        assertTrue(saved.getDetail().contains(visitId.toString()));
    }

    @Test
    @DisplayName("writePaymentDenied should catch RuntimeException from repository gracefully without failing caller")
    void writePaymentDeniedCatchesRepositoryExceptions() {
        AuditLogRepository auditLogRepository = mock(AuditLogRepository.class);
        doThrow(new RuntimeException("DB Connection Timeout")).when(auditLogRepository).save(any());

        BillingAccessDeniedAuditWriter writer = new BillingAccessDeniedAuditWriter(auditLogRepository, new ObjectMapper());

        assertDoesNotThrow(() -> writer.writePaymentDenied(
                UUID.randomUUID(), UUID.randomUUID(), Instant.now(), "Error test"
        ));
    }
}

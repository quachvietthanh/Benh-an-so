package com.benhsoan.application.ucservice.billing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.benhsoan.domain.auditlog.AuditLog;
import com.benhsoan.domain.auditlog.enums.ActionType;
import com.benhsoan.domain.auditlog.enums.ResourceType;
import com.benhsoan.domain.billing.Invoice;
import com.benhsoan.domain.billing.InvoiceLine;
import com.benhsoan.domain.billing.enums.InvoiceLineType;
import com.benhsoan.domain.billing.enums.InvoiceType;
import com.benhsoan.port.outbound.repository.audit.AuditLogRepository;
import com.benhsoan.port.outbound.repository.billing.InvoiceRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.benhsoan.port.outbound.time.ClockPort;

class RecordInvoiceReprintServiceTest {

    private static final Instant NOW = Instant.parse("2026-08-20T02:00:00Z");
    private static final UUID ACTOR_ID = UUID.randomUUID();

    private final InvoiceRepository invoiceRepository = mock(InvoiceRepository.class);
    private final InvoiceResultMapper resultMapper = mock(InvoiceResultMapper.class);
    private final ClockPort clockPort = mock(ClockPort.class);
    private final CurrentUserPort currentUserPort = mock(CurrentUserPort.class);
    private final AuditLogRepository auditLogRepository = mock(AuditLogRepository.class);

    private RecordInvoiceReprintService service;

    @BeforeEach
    void setUp() {
        service = new RecordInvoiceReprintService(
                invoiceRepository, resultMapper, clockPort, currentUserPort, auditLogRepository);
        when(clockPort.now()).thenReturn(NOW);
        when(currentUserPort.getCurrentUserId()).thenReturn(ACTOR_ID);
    }

    @Test
    void firstReprintIncrementsCountAndWritesIndividualAuditEvent() {
        Invoice invoice = invoice(0, null);
        stubPersistence(invoice);

        service.recordReprint(invoice.getId());

        assertEquals(1, invoice.getReprintCount());
        assertEquals(NOW, invoice.getLastReprintedAt());

        AuditLog auditLog = capturedAuditLog();
        assertEquals(ACTOR_ID, auditLog.getUserId());
        assertEquals(ActionType.REPRINT, auditLog.getActionType());
        assertEquals(ResourceType.INVOICE, auditLog.getResourceType());
        assertEquals(invoice.getId(), auditLog.getResourceId());
        assertEquals(NOW, auditLog.getCreatedAt());

        verify(invoiceRepository, never()).save(any(Invoice.class));
        verify(invoiceRepository).updateReprintMetadata(invoice.getId(), 1, NOW);
    }

    @Test
    void secondReprintIncrementsCountAndUpdatesTimestamp() {
        Instant firstReprint = Instant.parse("2026-08-20T01:00:00Z");
        Invoice invoice = invoice(1, firstReprint);
        stubPersistence(invoice);

        service.recordReprint(invoice.getId());

        assertEquals(2, invoice.getReprintCount());
        assertEquals(NOW, invoice.getLastReprintedAt());
        verify(auditLogRepository, times(1)).save(any(AuditLog.class));
    }

    @Test
    void eachReprintWritesItsOwnAuditEvent() {
        Invoice invoice = invoice(0, null);
        stubPersistence(invoice);

        service.recordReprint(invoice.getId());
        service.recordReprint(invoice.getId());

        verify(auditLogRepository, times(2)).save(any(AuditLog.class));
    }

    @Test
    void doesNotWriteAuditWhenPersistenceFails() {
        Invoice invoice = invoice(0, null);
        when(invoiceRepository.findById(invoice.getId())).thenReturn(Optional.of(invoice));
        doThrow(new RuntimeException("db error"))
                .when(invoiceRepository).updateReprintMetadata(any(), anyInt(), any());

        assertThrows(RuntimeException.class, () -> service.recordReprint(invoice.getId()));

        verify(auditLogRepository, never()).save(any(AuditLog.class));
    }

    private void stubPersistence(Invoice invoice) {
        when(invoiceRepository.findById(invoice.getId())).thenReturn(Optional.of(invoice));
    }

    private AuditLog capturedAuditLog() {
        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        return captor.getValue();
    }

    private Invoice invoice(int reprintCount, Instant lastReprintedAt) {
        UUID id = UUID.randomUUID();
        UUID visitId = UUID.randomUUID();
        InvoiceLine line = InvoiceLine.create(
                UUID.randomUUID(), id, InvoiceLineType.EXAM_FEE, "Phi kham",
                visitId, 1, new BigDecimal("250000"), new BigDecimal("250000"), NOW);
        return Invoice.restore(
                id, "HD000010", visitId, UUID.randomUUID(), InvoiceType.ORIGINAL,
                null, null, new BigDecimal("250000"), ACTOR_ID, NOW,
                reprintCount, lastReprintedAt, List.of(line));
    }
}

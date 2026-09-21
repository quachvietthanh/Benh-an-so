package com.benhsoan.application.ucservice.billing;

import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.domain.auditlog.AuditLog;
import com.benhsoan.domain.auditlog.enums.ActionType;
import com.benhsoan.domain.auditlog.enums.ResourceType;
import com.benhsoan.domain.billing.Invoice;
import com.benhsoan.domain.billing.exception.InvoiceNotFoundException;
import com.benhsoan.domain.shared.exception.ValidationException;
import com.benhsoan.port.dto.result.InvoiceResult;
import com.benhsoan.port.inbound.billing.RecordInvoiceReprintUseCase;
import com.benhsoan.port.outbound.repository.audit.AuditLogRepository;
import com.benhsoan.port.outbound.repository.billing.InvoiceRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.benhsoan.port.outbound.time.ClockPort;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class RecordInvoiceReprintService implements RecordInvoiceReprintUseCase {

    private final InvoiceRepository invoiceRepository;
    private final InvoiceResultMapper resultMapper;
    private final ClockPort clockPort;
    private final CurrentUserPort currentUserPort;
    private final AuditLogRepository auditLogRepository;

    @Override
    public InvoiceResult recordReprint(UUID invoiceId) {
        if (invoiceId == null) {
            throw new ValidationException("Invoice id is required.");
        }

        Instant now = clockPort.now();
        UUID actorId = currentUserPort.getCurrentUserId();
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new InvoiceNotFoundException(invoiceId));

        invoice.recordReprint(now);
        invoiceRepository.updateReprintMetadata(
                invoice.getId(), invoice.getReprintCount(), invoice.getLastReprintedAt());

        auditLogRepository.save(AuditLog.create(
                actorId,
                ActionType.REPRINT,
                ResourceType.INVOICE,
                invoice.getId(),
                "{\"invoiceCode\":\"%s\",\"reprintCount\":%d}".formatted(
                        invoice.getInvoiceCode(), invoice.getReprintCount()),
                null,
                now));

        return resultMapper.toResult(invoice);
    }
}

package com.benhsoan.application.ucservice.billing;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.domain.auditlog.AuditLog;
import com.benhsoan.domain.auditlog.enums.ActionType;
import com.benhsoan.domain.auditlog.enums.ResourceType;
import com.benhsoan.domain.billing.Invoice;
import com.benhsoan.domain.billing.InvoiceLine;
import com.benhsoan.domain.billing.enums.InvoiceLineType;
import com.benhsoan.domain.billing.exception.InvoiceNotFoundException;
import com.benhsoan.domain.shared.exception.ValidationException;
import com.benhsoan.port.dto.command.billing.AdjustInvoiceCommand;
import com.benhsoan.port.dto.command.billing.AdjustmentInvoiceLineCommand;
import com.benhsoan.port.dto.result.InvoiceResult;
import com.benhsoan.port.inbound.billing.AdjustInvoiceUseCase;
import com.benhsoan.port.outbound.generator.AdjustmentInvoiceCodeGenerator;
import com.benhsoan.port.outbound.repository.audit.AuditLogRepository;
import com.benhsoan.port.outbound.repository.billing.InvoiceRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.benhsoan.port.outbound.time.ClockPort;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class AdjustInvoiceService implements AdjustInvoiceUseCase {

    private final InvoiceRepository invoiceRepository;
    private final AdjustmentInvoiceCodeGenerator adjustmentInvoiceCodeGenerator;
    private final CurrentUserPort currentUserPort;
    private final ClockPort clockPort;
    private final AuditLogRepository auditLogRepository;
    private final InvoiceResultMapper resultMapper;

    @Override
    public InvoiceResult adjust(AdjustInvoiceCommand command) {
        validateCommand(command);

        Invoice originalInvoice = invoiceRepository.findById(command.originalInvoiceId())
                .orElseThrow(() -> new InvoiceNotFoundException(command.originalInvoiceId()));

        if (!originalInvoice.isOriginal()) {
            throw new ValidationException("Only original invoices can be adjusted.");
        }

        UUID actorId = currentUserPort.getCurrentUserId();
        Instant now = clockPort.now();
        UUID adjustmentInvoiceId = UUID.randomUUID();

        List<InvoiceLine> lines = command.lines()
                .stream()
                .map(line -> toAdjustmentLine(adjustmentInvoiceId, originalInvoice.getId(), now, line))
                .toList();

        Invoice adjustmentInvoice = Invoice.createAdjustment(
                adjustmentInvoiceId,
                adjustmentInvoiceCodeGenerator.generate(),
                originalInvoice.getVisitId(),
                originalInvoice.getId(),
                command.adjustmentReason(),
                actorId,
                now,
                lines,
                currentUserPort.hasRole("ADMIN")
        );

        Invoice saved = invoiceRepository.save(adjustmentInvoice);

        auditLogRepository.save(
                AuditLog.create(
                        actorId,
                        ActionType.CREATE,
                        ResourceType.INVOICE,
                        saved.getId(),
                        """
                        {
                        "invoiceCode":"%s",
                        "originalInvoiceId":"%s",
                        "visitId":"%s",
                        "totalAmount":"%s",
                        "adjustmentReason":"%s"
                        }
                        """.formatted(
                                saved.getInvoiceCode(),
                                saved.getOriginalInvoiceId(),
                                saved.getVisitId(),
                                saved.getTotalAmount(),
                                saved.getAdjustmentReason()
                        ),
                        null
                )
        );

        return resultMapper.toResult(saved);
    }

    private void validateCommand(AdjustInvoiceCommand command) {
        if (command.originalInvoiceId() == null) {
            throw new ValidationException("Original invoice id is required.");
        }
        if (command.lines() == null || command.lines().isEmpty()) {
            throw new ValidationException("Adjustment invoice must contain at least one line.");
        }
    }

    private InvoiceLine toAdjustmentLine(
            UUID adjustmentInvoiceId,
            UUID originalInvoiceId,
            Instant now,
            AdjustmentInvoiceLineCommand line
    ) {
        if (line == null) {
            throw new ValidationException("Adjustment invoice line is required.");
        }

        BigDecimal amount = line.unitPrice().multiply(BigDecimal.valueOf(line.quantity()));
        UUID referenceId = line.referenceId() != null ? line.referenceId() : originalInvoiceId;

        return InvoiceLine.create(
                UUID.randomUUID(),
                adjustmentInvoiceId,
                InvoiceLineType.ADJUSTMENT,
                line.itemName(),
                referenceId,
                line.quantity(),
                line.unitPrice(),
                amount,
                now
        );
    }
}

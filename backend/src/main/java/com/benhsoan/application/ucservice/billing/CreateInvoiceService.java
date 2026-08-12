package com.benhsoan.application.ucservice.billing;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.domain.auditlog.AuditLog;
import com.benhsoan.domain.auditlog.enums.ActionType;
import com.benhsoan.domain.auditlog.enums.ResourceType;
import com.benhsoan.domain.billing.Invoice;
import com.benhsoan.domain.billing.InvoiceLine;
import com.benhsoan.domain.billing.Payment;
import com.benhsoan.domain.billing.enums.InvoiceLineType;
import com.benhsoan.domain.billing.enums.PaymentStatus;
import com.benhsoan.domain.billing.exception.PaymentNotFoundException;
import com.benhsoan.domain.shared.exception.ValidationException;
import com.benhsoan.port.dto.command.billing.CreateInvoiceCommand;
import com.benhsoan.port.dto.result.InvoiceResult;
import com.benhsoan.port.inbound.billing.CreateInvoiceUseCase;
import com.benhsoan.port.outbound.generator.InvoiceCodeGenerator;
import com.benhsoan.port.outbound.repository.audit.AuditLogRepository;
import com.benhsoan.port.outbound.repository.billing.InvoiceRepository;
import com.benhsoan.port.outbound.repository.billing.PaymentRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.benhsoan.port.outbound.time.ClockPort;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class CreateInvoiceService implements CreateInvoiceUseCase {

    private final PaymentRepository paymentRepository;
    private final InvoiceRepository invoiceRepository;
    private final InvoiceCodeGenerator invoiceCodeGenerator;
    private final CurrentUserPort currentUserPort;
    private final ClockPort clockPort;
    private final AuditLogRepository auditLogRepository;
    private final InvoiceResultMapper resultMapper;

    @Override
    public InvoiceResult create(CreateInvoiceCommand command) {
        ensureAuthorized();
        validateCommand(command);

        Payment payment = resolvePayment(command);

        invoiceRepository.findOriginalByVisitId(payment.getVisitId())
                .ifPresent(existing -> {
                    throw new com.benhsoan.domain.billing.exception.InvoiceAlreadyIssuedException(
                            payment.getVisitId()
                    );
                });

        UUID actorId = currentUserPort.getCurrentUserId();
        Instant now = clockPort.now();
        UUID invoiceId = UUID.randomUUID();

        List<InvoiceLine> lines = List.of(
                InvoiceLine.create(
                        UUID.randomUUID(),
                        invoiceId,
                        InvoiceLineType.EXAM_FEE,
                        "Phi kham",
                        payment.getVisitId(),
                        1,
                        payment.getExamFee(),
                        payment.getExamFee(),
                        now
                ),
                InvoiceLine.create(
                        UUID.randomUUID(),
                        invoiceId,
                        InvoiceLineType.MEDICINE_FEE,
                        "Tien thuoc",
                        payment.getId(),
                        1,
                        payment.getMedicineFee(),
                        payment.getMedicineFee(),
                        now
                )
        );

        boolean paymentRecorded = payment.getStatus() == PaymentStatus.RECORDED
                || payment.getStatus() == PaymentStatus.SUCCESS;

        Invoice invoice = Invoice.createOriginal(
                invoiceId,
                invoiceCodeGenerator.generate(),
                payment.getVisitId(),
                payment.getId(),
                actorId,
                now,
                lines,
                paymentRecorded,
                false
        );

        Invoice saved = invoiceRepository.save(invoice);

        auditLogRepository.save(
                AuditLog.create(
                        actorId,
                        ActionType.CREATE,
                        ResourceType.INVOICE,
                        saved.getId(),
                        """
                        {
                        "invoiceCode":"%s",
                        "visitId":"%s",
                        "paymentId":"%s",
                        "totalAmount":"%s"
                        }
                        """.formatted(
                                saved.getInvoiceCode(),
                                saved.getVisitId(),
                                saved.getPaymentId(),
                                saved.getTotalAmount()
                        ),
                        null
                )
        );

        return resultMapper.toResult(saved);
    }

    private void ensureAuthorized() {
        if (!currentUserPort.hasRole("ADMIN")
                && !currentUserPort.hasRole("RECEPTIONIST")) {
            throw new AccessDeniedException("Only receptionists can create invoices.");
        }
    }

    private void validateCommand(CreateInvoiceCommand command) {
        boolean hasVisitId = command.visitId() != null;
        boolean hasPaymentId = command.paymentId() != null;

        if (!hasVisitId && !hasPaymentId) {
            throw new ValidationException("Either visit id or payment id is required.");
        }
    }

    private Payment resolvePayment(CreateInvoiceCommand command) {
        if (command.paymentId() != null) {
            return paymentRepository.findById(command.paymentId())
                    .orElseThrow(() -> new PaymentNotFoundException(command.paymentId()));
        }

        return paymentRepository.findByVisitId(command.visitId())
                .orElseThrow(() -> new PaymentNotFoundException(command.visitId(), true));
    }
}

package com.benhsoan.application.ucservice.billing;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.dao.DataIntegrityViolationException;

import com.benhsoan.domain.auditlog.AuditLog;
import com.benhsoan.domain.auditlog.enums.ActionType;
import com.benhsoan.domain.auditlog.enums.ResourceType;
import com.benhsoan.domain.billing.Invoice;
import com.benhsoan.domain.billing.InvoiceLine;
import com.benhsoan.domain.billing.Payment;
import com.benhsoan.domain.billing.PaymentServiceFee;
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
import com.benhsoan.port.outbound.repository.billing.PaymentServiceFeeRepository;
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
    private final PaymentServiceFeeRepository paymentServiceFeeRepository;

    @Override
    public InvoiceResult create(CreateInvoiceCommand command) {
        ensureAuthorized();
        Payment payment = resolvePayment(command);

        UUID actorId = currentUserPort.getCurrentUserId();
        Instant now = clockPort.now();
        UUID invoiceId = UUID.randomUUID();
        List<PaymentServiceFee> serviceFees = paymentServiceFeeRepository
                .findAllByPaymentId(payment.getId());
        validateCollectedServiceFee(payment, serviceFees);

        List<InvoiceLine> lines = buildInvoiceLines(payment, invoiceId, now, serviceFees);

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

        Invoice saved;
        try {
            saved = invoiceRepository.save(invoice);
        } catch (DataIntegrityViolationException ex) {
            if (isDuplicateOriginalInvoiceConflict(ex)) {
                throw new com.benhsoan.domain.billing.exception.InvoiceAlreadyIssuedException(
                        payment.getVisitId()
                );
            }
            throw ex;
        }

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

    private Payment resolvePayment(CreateInvoiceCommand command) {
        boolean hasVisitId = command.visitId() != null;
        boolean hasPaymentId = command.paymentId() != null;

        if (!hasVisitId && !hasPaymentId) {
            throw new ValidationException("Either visit id or payment id is required.");
        }

        if (command.paymentId() != null) {
            Payment payment = paymentRepository.findById(command.paymentId())
                    .orElseThrow(() -> new PaymentNotFoundException(command.paymentId()));

            if (hasVisitId && !payment.getVisitId().equals(command.visitId())) {
                throw new ValidationException("visitId does not match paymentId.");
            }

            invoiceRepository.findOriginalByVisitId(payment.getVisitId())
                    .ifPresent(existing -> {
                        throw new com.benhsoan.domain.billing.exception.InvoiceAlreadyIssuedException(
                                payment.getVisitId()
                        );
                    });

            return payment;
        }

        Payment payment = paymentRepository.findByVisitId(command.visitId())
                .orElseThrow(() -> new PaymentNotFoundException(command.visitId(), true));

        invoiceRepository.findOriginalByVisitId(payment.getVisitId())
                .ifPresent(existing -> {
                    throw new com.benhsoan.domain.billing.exception.InvoiceAlreadyIssuedException(
                            payment.getVisitId()
                    );
                });

        return payment;
    }

    private List<InvoiceLine> buildInvoiceLines(
            Payment payment,
            UUID invoiceId,
            Instant now,
            List<PaymentServiceFee> serviceFees
    ) {
        List<InvoiceLine> lines = new ArrayList<>();

        if (payment.getExamFee().compareTo(java.math.BigDecimal.ZERO) > 0) {
            lines.add(InvoiceLine.create(
                    UUID.randomUUID(),
                    invoiceId,
                    InvoiceLineType.EXAM_FEE,
                    "Phi kham",
                    payment.getVisitId(),
                    1,
                    payment.getExamFee(),
                    payment.getExamFee(),
                    now
            ));
        }

        if (payment.getMedicineFee().compareTo(java.math.BigDecimal.ZERO) > 0) {
            lines.add(InvoiceLine.create(
                    UUID.randomUUID(),
                    invoiceId,
                    InvoiceLineType.MEDICINE_FEE,
                    "Tien thuoc",
                    payment.getId(),
                    1,
                    payment.getMedicineFee(),
                    payment.getMedicineFee(),
                    now
            ));
        }

        serviceFees.stream()
                .filter(fee -> fee.getAmount().compareTo(java.math.BigDecimal.ZERO) > 0)
                .map(fee -> InvoiceLine.create(
                        UUID.randomUUID(),
                        invoiceId,
                        InvoiceLineType.SERVICE_FEE,
                        fee.getServiceName(),
                        fee.getClinicalOrderItemId(),
                        1,
                        fee.getAmount(),
                        fee.getAmount(),
                        now
                ))
                .forEach(lines::add);

        if (lines.isEmpty()) {
            throw new ValidationException("Invoice must contain at least one non-zero charge line.");
        }

        return List.copyOf(lines);
    }

    private void validateCollectedServiceFee(
            Payment payment,
            List<PaymentServiceFee> serviceFees
    ) {
        if (payment.getServiceFee().compareTo(
                serviceFees.stream()
                        .map(PaymentServiceFee::getAmount)
                        .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add)
        ) != 0) {
            throw new ValidationException(
                    "Stored clinical service fee snapshots do not match the payment total."
            );
        }
    }

    private boolean isDuplicateOriginalInvoiceConflict(DataIntegrityViolationException ex) {
        String message = extractMessage(ex).toLowerCase();
        return message.contains("uk_invoices_payment")
                || message.contains("duplicate entry")
                && message.contains("payment_id");
    }

    private String extractMessage(Throwable throwable) {
        StringBuilder builder = new StringBuilder();
        Throwable current = throwable;
        while (current != null) {
            if (current.getMessage() != null) {
                builder.append(current.getMessage()).append(' ');
            }
            current = current.getCause();
        }
        return builder.toString();
    }
}

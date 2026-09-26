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
import com.benhsoan.domain.billing.exception.InvoiceNotFoundException;
import com.benhsoan.domain.billing.exception.PaymentNotAllowedException;
import com.benhsoan.domain.billing.exception.PaymentNotFoundException;
import com.benhsoan.domain.prescription.enums.PrescriptionStatus;
import com.benhsoan.domain.shared.exception.ValidationException;
import com.benhsoan.port.dto.command.billing.RefundPaymentCommand;
import com.benhsoan.port.dto.result.RefundPaymentResult;
import com.benhsoan.port.inbound.billing.RefundPaymentUseCase;
import com.benhsoan.port.outbound.generator.AdjustmentInvoiceCodeGenerator;
import com.benhsoan.port.outbound.repository.audit.AuditLogRepository;
import com.benhsoan.port.outbound.repository.billing.InvoiceRepository;
import com.benhsoan.port.outbound.repository.billing.PaymentRepository;
import com.benhsoan.port.outbound.repository.medicalrecord.MedicalRecordRepository;
import com.benhsoan.port.outbound.repository.prescription.PrescriptionRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.benhsoan.port.outbound.time.ClockPort;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class RefundPaymentService implements RefundPaymentUseCase {

    private final PaymentRepository paymentRepository;
    private final InvoiceRepository invoiceRepository;
    private final MedicalRecordRepository medicalRecordRepository;
    private final PrescriptionRepository prescriptionRepository;
    private final AdjustmentInvoiceCodeGenerator adjustmentInvoiceCodeGenerator;
    private final CurrentUserPort currentUserPort;
    private final ClockPort clockPort;
    private final AuditLogRepository auditLogRepository;
    private final InvoiceResultMapper invoiceResultMapper;

    @Override
    public RefundPaymentResult refund(RefundPaymentCommand command) {
        ensureAuthorized();
        UUID paymentId = requirePaymentId(command);

        Payment payment = paymentRepository.findByIdForUpdate(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException(paymentId));

        UUID actorId = currentUserPort.getCurrentUserId();
        Instant now = clockPort.now();
        payment.refund(command.reason(), actorId, now);

        ensureNoDispensedPrescription(payment.getVisitId());

        Invoice originalInvoice = invoiceRepository.findByPaymentId(paymentId)
                .orElseThrow(() -> new InvoiceNotFoundException(paymentId, true));
        if (!originalInvoice.isOriginal()) {
            throw new PaymentNotAllowedException(
                    "Refund requires an original invoice linked to the payment."
            );
        }
        if (invoiceRepository.existsByOriginalInvoiceId(originalInvoice.getId())) {
            throw new PaymentNotAllowedException(
                    "Payments with an adjusted invoice cannot be refunded."
            );
        }

        Invoice adjustmentInvoice = createRefundAdjustment(
                originalInvoice,
                payment.getRefundReason(),
                actorId,
                now
        );

        Payment savedPayment = paymentRepository.save(payment);
        Invoice savedAdjustment = invoiceRepository.save(adjustmentInvoice);
        saveAuditLogs(savedPayment, savedAdjustment, actorId, now);

        return new RefundPaymentResult(
                savedPayment.getId(),
                savedPayment.getVisitId(),
                savedPayment.getStatus(),
                savedPayment.getAmountPaid(),
                savedPayment.getRefundReason(),
                savedPayment.getRefundedBy(),
                savedPayment.getRefundedAt(),
                invoiceResultMapper.toResult(savedAdjustment)
        );
    }

    private void ensureAuthorized() {
        if (!currentUserPort.hasRole("MANAGER")) {
            throw new AccessDeniedException("Only managers can refund payments.");
        }
    }

    private UUID requirePaymentId(RefundPaymentCommand command) {
        if (command == null || command.paymentId() == null) {
            throw new ValidationException("Payment id is required.");
        }
        return command.paymentId();
    }

    private void ensureNoDispensedPrescription(UUID visitId) {
        medicalRecordRepository.findByVisitId(visitId).ifPresent(record -> {
            boolean hasDispensedPrescription = prescriptionRepository
                    .findByMedicalRecordId(record.getId())
                    .stream()
                    .anyMatch(prescription ->
                            prescription.getStatus() == PrescriptionStatus.DISPENSED
                    );
            if (hasDispensedPrescription) {
                throw new PaymentNotAllowedException(
                        "Payments for dispensed prescriptions cannot be refunded; "
                                + "create an invoice adjustment instead."
                );
            }
        });
    }

    private Invoice createRefundAdjustment(
            Invoice originalInvoice,
            String reason,
            UUID actorId,
            Instant now
    ) {
        UUID adjustmentInvoiceId = UUID.randomUUID();
        List<InvoiceLine> lines = originalInvoice.getLines().stream()
                .map(line -> InvoiceLine.create(
                        UUID.randomUUID(),
                        adjustmentInvoiceId,
                        InvoiceLineType.ADJUSTMENT,
                        line.getItemName(),
                        line.getReferenceId(),
                        line.getQuantity(),
                        line.getUnitPrice().negate(),
                        line.getAmount().negate(),
                        now
                ))
                .toList();

        return Invoice.createAdjustment(
                adjustmentInvoiceId,
                adjustmentInvoiceCodeGenerator.generate(),
                originalInvoice.getVisitId(),
                originalInvoice.getId(),
                reason,
                actorId,
                now,
                lines,
                true
        );
    }

    private void saveAuditLogs(
            Payment payment,
            Invoice adjustmentInvoice,
            UUID actorId,
            Instant now
    ) {
        auditLogRepository.save(AuditLog.create(
                actorId,
                ActionType.UPDATE,
                ResourceType.PAYMENT,
                payment.getId(),
                """
                {
                "status":"REFUNDED",
                "amountRefunded":"%s",
                "refundReason":"%s",
                "refundedAt":"%s",
                "adjustmentInvoiceId":"%s"
                }
                """.formatted(
                        payment.getAmountPaid(),
                        payment.getRefundReason(),
                        payment.getRefundedAt(),
                        adjustmentInvoice.getId()
                ),
                null,
                now
        ));
        auditLogRepository.save(AuditLog.create(
                actorId,
                ActionType.CREATE,
                ResourceType.INVOICE,
                adjustmentInvoice.getId(),
                """
                {
                "invoiceCode":"%s",
                "originalInvoiceId":"%s",
                "paymentId":"%s",
                "totalAmount":"%s",
                "adjustmentReason":"%s"
                }
                """.formatted(
                        adjustmentInvoice.getInvoiceCode(),
                        adjustmentInvoice.getOriginalInvoiceId(),
                        payment.getId(),
                        adjustmentInvoice.getTotalAmount(),
                        adjustmentInvoice.getAdjustmentReason()
                ),
                null,
                now
        ));
    }
}

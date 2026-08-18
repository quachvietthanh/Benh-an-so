package com.benhsoan.application.ucservice.billing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
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

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.access.AccessDeniedException;

import com.benhsoan.domain.auditlog.AuditLog;
import com.benhsoan.domain.auditlog.enums.ActionType;
import com.benhsoan.domain.auditlog.enums.ResourceType;
import com.benhsoan.domain.billing.Invoice;
import com.benhsoan.domain.billing.InvoiceLine;
import com.benhsoan.domain.billing.Payment;
import com.benhsoan.domain.billing.enums.InvoiceLineType;
import com.benhsoan.domain.billing.enums.InvoiceType;
import com.benhsoan.domain.billing.enums.PaymentMethod;
import com.benhsoan.domain.billing.enums.PaymentStatus;
import com.benhsoan.domain.billing.exception.InvoiceNotFoundException;
import com.benhsoan.domain.billing.exception.PaymentNotAllowedException;
import com.benhsoan.domain.billing.exception.PaymentNotFoundException;
import com.benhsoan.domain.medicalrecord.MedicalRecord;
import com.benhsoan.domain.prescription.Prescription;
import com.benhsoan.domain.prescription.enums.PrescriptionStatus;
import com.benhsoan.domain.shared.exception.ValidationException;
import com.benhsoan.port.dto.command.billing.RefundPaymentCommand;
import com.benhsoan.port.dto.result.RefundPaymentResult;
import com.benhsoan.port.outbound.generator.AdjustmentInvoiceCodeGenerator;
import com.benhsoan.port.outbound.repository.audit.AuditLogRepository;
import com.benhsoan.port.outbound.repository.billing.InvoiceRepository;
import com.benhsoan.port.outbound.repository.billing.PaymentRepository;
import com.benhsoan.port.outbound.repository.medicalrecord.MedicalRecordRepository;
import com.benhsoan.port.outbound.repository.prescription.PrescriptionRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.benhsoan.port.outbound.time.ClockPort;

class RefundPaymentServiceTest {

    private static final Instant REFUNDED_AT = Instant.parse("2026-08-18T04:00:00Z");

    @Test
    void refundsPaymentAndCreatesFullNegativeAdjustment() {
        Fixture fixture = fixture(PaymentStatus.SUCCESS);
        MedicalRecord medicalRecord = mock(MedicalRecord.class);
        Prescription cancelledPrescription = mock(Prescription.class);
        when(medicalRecord.getId()).thenReturn(UUID.randomUUID());
        when(cancelledPrescription.getStatus()).thenReturn(PrescriptionStatus.CANCELLED);
        when(fixture.medicalRecordRepository.findByVisitId(fixture.payment.getVisitId()))
                .thenReturn(Optional.of(medicalRecord));
        when(fixture.prescriptionRepository.findByMedicalRecordId(medicalRecord.getId()))
                .thenReturn(List.of(cancelledPrescription));

        RefundPaymentResult result = fixture.service.refund(
                new RefundPaymentCommand(fixture.payment.getId(), "  Patient cancelled  ")
        );

        assertEquals(PaymentStatus.REFUNDED, result.status());
        assertEquals(new BigDecimal("250000"), result.amountRefunded());
        assertEquals("Patient cancelled", result.refundReason());
        assertEquals(fixture.actorId, result.refundedBy());
        assertEquals(REFUNDED_AT, result.refundedAt());
        assertEquals(InvoiceType.ADJUSTMENT, result.adjustmentInvoice().type());
        assertEquals(fixture.originalInvoice.getId(), result.adjustmentInvoice().originalInvoiceId());
        assertEquals(REFUNDED_AT, result.adjustmentInvoice().createdAt());
        assertEquals(new BigDecimal("-250000"), result.adjustmentInvoice().totalAmount());
        assertEquals(2, result.adjustmentInvoice().lines().size());
        assertEquals(new BigDecimal("-100000"), result.adjustmentInvoice().lines().get(0).amount());
        assertEquals(new BigDecimal("-150000"), result.adjustmentInvoice().lines().get(1).amount());
        assertEquals(InvoiceLineType.ADJUSTMENT, result.adjustmentInvoice().lines().get(0).lineType());

        verify(fixture.paymentRepository).findByIdForUpdate(fixture.payment.getId());
        verify(fixture.paymentRepository).save(fixture.payment);
        verify(fixture.invoiceRepository).save(any(Invoice.class));

        ArgumentCaptor<AuditLog> auditCaptor = ArgumentCaptor.forClass(AuditLog.class);
        verify(fixture.auditLogRepository, times(2)).save(auditCaptor.capture());
        List<AuditLog> audits = auditCaptor.getAllValues();
        assertEquals(ActionType.UPDATE, audits.get(0).getActionType());
        assertEquals(ResourceType.PAYMENT, audits.get(0).getResourceType());
        assertEquals(ActionType.CREATE, audits.get(1).getActionType());
        assertEquals(ResourceType.INVOICE, audits.get(1).getResourceType());
        assertEquals(fixture.actorId, audits.get(0).getUserId());
        assertEquals(fixture.actorId, audits.get(1).getUserId());
        assertEquals(fixture.payment.getId(), audits.get(0).getResourceId());
        assertEquals(result.adjustmentInvoice().id(), audits.get(1).getResourceId());
        assertEquals(REFUNDED_AT, audits.get(0).getCreatedAt());
        assertEquals(REFUNDED_AT, audits.get(1).getCreatedAt());
        assertTrue(audits.get(0).getDetail().contains("\"refundReason\":\"Patient cancelled\""));
        assertTrue(audits.get(0).getDetail().contains("\"refundedAt\":\"" + REFUNDED_AT + "\""));
        assertTrue(audits.get(1).getDetail().contains("\"adjustmentReason\":\"Patient cancelled\""));
    }

    @Test
    void rejectsUnauthorizedActorBeforeLockingPayment() {
        Fixture fixture = fixture(PaymentStatus.SUCCESS);
        when(fixture.currentUserPort.hasRole("MANAGER")).thenReturn(false);

        assertThrows(
                AccessDeniedException.class,
                () -> fixture.service.refund(command(fixture.payment.getId()))
        );

        verify(fixture.paymentRepository, never()).findByIdForUpdate(any());
        verifyNoWrites(fixture);
    }

    @Test
    void rejectsMissingPaymentId() {
        Fixture fixture = fixture(PaymentStatus.SUCCESS);

        assertThrows(
                ValidationException.class,
                () -> fixture.service.refund(new RefundPaymentCommand(null, "Reason"))
        );

        verify(fixture.paymentRepository, never()).findByIdForUpdate(any());
        verifyNoWrites(fixture);
    }

    @Test
    void rejectsMissingPayment() {
        Fixture fixture = fixture(PaymentStatus.SUCCESS);
        when(fixture.paymentRepository.findByIdForUpdate(fixture.payment.getId()))
                .thenReturn(Optional.empty());

        assertThrows(
                PaymentNotFoundException.class,
                () -> fixture.service.refund(command(fixture.payment.getId()))
        );

        verifyNoWrites(fixture);
    }

    @Test
    void rejectsDispensedPrescriptionBeforeInvoiceLookup() {
        Fixture fixture = fixture(PaymentStatus.SUCCESS);
        MedicalRecord medicalRecord = mock(MedicalRecord.class);
        Prescription dispensedPrescription = mock(Prescription.class);
        when(medicalRecord.getId()).thenReturn(UUID.randomUUID());
        when(dispensedPrescription.getStatus()).thenReturn(PrescriptionStatus.DISPENSED);
        when(fixture.medicalRecordRepository.findByVisitId(fixture.payment.getVisitId()))
                .thenReturn(Optional.of(medicalRecord));
        when(fixture.prescriptionRepository.findByMedicalRecordId(medicalRecord.getId()))
                .thenReturn(List.of(dispensedPrescription));

        assertThrows(
                PaymentNotAllowedException.class,
                () -> fixture.service.refund(command(fixture.payment.getId()))
        );

        verify(fixture.invoiceRepository, never()).findByPaymentId(any());
        verifyNoWrites(fixture);
    }

    @Test
    void rejectsMissingOriginalInvoice() {
        Fixture fixture = fixture(PaymentStatus.SUCCESS);
        when(fixture.invoiceRepository.findByPaymentId(fixture.payment.getId()))
                .thenReturn(Optional.empty());

        assertThrows(
                InvoiceNotFoundException.class,
                () -> fixture.service.refund(command(fixture.payment.getId()))
        );

        verifyNoWrites(fixture);
    }

    @Test
    void rejectsInvoiceThatWasAlreadyAdjusted() {
        Fixture fixture = fixture(PaymentStatus.SUCCESS);
        when(fixture.invoiceRepository.existsByOriginalInvoiceId(fixture.originalInvoice.getId()))
                .thenReturn(true);

        assertThrows(
                PaymentNotAllowedException.class,
                () -> fixture.service.refund(command(fixture.payment.getId()))
        );

        verifyNoWrites(fixture);
    }

    @Test
    void rejectsNonRefundablePaymentStatusWithoutWrites() {
        Fixture fixture = fixture(PaymentStatus.CANCELLED);

        assertThrows(
                PaymentNotAllowedException.class,
                () -> fixture.service.refund(command(fixture.payment.getId()))
        );

        verifyNoWrites(fixture);
    }

    @Test
    void rejectsAlreadyRefundedPaymentWithoutWrites() {
        Fixture fixture = fixture(PaymentStatus.REFUNDED);

        assertThrows(
                PaymentNotAllowedException.class,
                () -> fixture.service.refund(command(fixture.payment.getId()))
        );

        verifyNoWrites(fixture);
    }

    @Test
    void rejectsBlankReasonWithoutWrites() {
        Fixture fixture = fixture(PaymentStatus.SUCCESS);

        assertThrows(
                ValidationException.class,
                () -> fixture.service.refund(
                        new RefundPaymentCommand(fixture.payment.getId(), " ")
                )
        );

        verifyNoWrites(fixture);
    }

    private static RefundPaymentCommand command(UUID paymentId) {
        return new RefundPaymentCommand(paymentId, "Patient cancelled");
    }

    private static Fixture fixture(PaymentStatus status) {
        PaymentRepository paymentRepository = mock(PaymentRepository.class);
        InvoiceRepository invoiceRepository = mock(InvoiceRepository.class);
        MedicalRecordRepository medicalRecordRepository = mock(MedicalRecordRepository.class);
        PrescriptionRepository prescriptionRepository = mock(PrescriptionRepository.class);
        AdjustmentInvoiceCodeGenerator codeGenerator = mock(AdjustmentInvoiceCodeGenerator.class);
        CurrentUserPort currentUserPort = mock(CurrentUserPort.class);
        ClockPort clockPort = mock(ClockPort.class);
        AuditLogRepository auditLogRepository = mock(AuditLogRepository.class);
        UUID actorId = UUID.randomUUID();
        Payment payment = payment(status);
        Invoice originalInvoice = originalInvoice(payment);

        when(currentUserPort.hasRole("MANAGER")).thenReturn(true);
        when(currentUserPort.getCurrentUserId()).thenReturn(actorId);
        when(clockPort.now()).thenReturn(REFUNDED_AT);
        when(paymentRepository.findByIdForUpdate(payment.getId()))
                .thenReturn(Optional.of(payment));
        when(medicalRecordRepository.findByVisitId(payment.getVisitId()))
                .thenReturn(Optional.empty());
        when(invoiceRepository.findByPaymentId(payment.getId()))
                .thenReturn(Optional.of(originalInvoice));
        when(invoiceRepository.existsByOriginalInvoiceId(originalInvoice.getId()))
                .thenReturn(false);
        when(codeGenerator.generate()).thenReturn("HDDC000020");
        when(paymentRepository.save(any(Payment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(invoiceRepository.save(any(Invoice.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        RefundPaymentService service = new RefundPaymentService(
                paymentRepository,
                invoiceRepository,
                medicalRecordRepository,
                prescriptionRepository,
                codeGenerator,
                currentUserPort,
                clockPort,
                auditLogRepository,
                new InvoiceResultMapper()
        );
        return new Fixture(
                service,
                paymentRepository,
                invoiceRepository,
                medicalRecordRepository,
                prescriptionRepository,
                currentUserPort,
                auditLogRepository,
                payment,
                originalInvoice,
                actorId
        );
    }

    private static Payment payment(PaymentStatus status) {
        Instant paidAt = Instant.parse("2026-08-17T03:00:00Z");
        return Payment.restore(
                UUID.randomUUID(),
                UUID.randomUUID(),
                new BigDecimal("100000"),
                new BigDecimal("150000"),
                new BigDecimal("250000"),
                new BigDecimal("250000"),
                PaymentMethod.CASH,
                status,
                UUID.randomUUID(),
                paidAt,
                paidAt
        );
    }

    private static Invoice originalInvoice(Payment payment) {
        UUID invoiceId = UUID.randomUUID();
        Instant createdAt = Instant.parse("2026-08-17T03:05:00Z");
        return Invoice.restore(
                invoiceId,
                "HD000020",
                payment.getVisitId(),
                payment.getId(),
                InvoiceType.ORIGINAL,
                null,
                null,
                new BigDecimal("250000"),
                UUID.randomUUID(),
                createdAt,
                List.of(
                        InvoiceLine.create(
                                UUID.randomUUID(),
                                invoiceId,
                                InvoiceLineType.EXAM_FEE,
                                "Exam fee",
                                payment.getVisitId(),
                                1,
                                new BigDecimal("100000"),
                                new BigDecimal("100000"),
                                createdAt
                        ),
                        InvoiceLine.create(
                                UUID.randomUUID(),
                                invoiceId,
                                InvoiceLineType.MEDICINE_FEE,
                                "Medicine fee",
                                payment.getId(),
                                1,
                                new BigDecimal("150000"),
                                new BigDecimal("150000"),
                                createdAt
                        )
                )
        );
    }

    private static void verifyNoWrites(Fixture fixture) {
        verify(fixture.paymentRepository, never()).save(any());
        verify(fixture.invoiceRepository, never()).save(any());
        verify(fixture.auditLogRepository, never()).save(any());
    }

    private record Fixture(
            RefundPaymentService service,
            PaymentRepository paymentRepository,
            InvoiceRepository invoiceRepository,
            MedicalRecordRepository medicalRecordRepository,
            PrescriptionRepository prescriptionRepository,
            CurrentUserPort currentUserPort,
            AuditLogRepository auditLogRepository,
            Payment payment,
            Invoice originalInvoice,
            UUID actorId
    ) {
    }
}

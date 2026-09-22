package com.benhsoan.application.ucservice.billing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;

import com.benhsoan.domain.billing.Invoice;
import com.benhsoan.domain.billing.Payment;
import com.benhsoan.domain.billing.PaymentServiceFee;
import com.benhsoan.domain.billing.enums.PaymentMethod;
import com.benhsoan.domain.billing.enums.PaymentStatus;
import com.benhsoan.domain.billing.enums.InvoiceLineType;
import com.benhsoan.domain.billing.exception.InvoiceAlreadyIssuedException;
import com.benhsoan.domain.billing.exception.PaymentNotFoundException;
import com.benhsoan.domain.shared.exception.ValidationException;
import com.benhsoan.port.dto.command.billing.CreateInvoiceCommand;
import com.benhsoan.port.dto.result.InvoiceResult;
import com.benhsoan.port.outbound.generator.InvoiceCodeGenerator;
import com.benhsoan.port.outbound.repository.audit.AuditLogRepository;
import com.benhsoan.port.outbound.repository.billing.InvoiceRepository;
import com.benhsoan.port.outbound.repository.billing.PaymentRepository;
import com.benhsoan.port.outbound.repository.billing.PaymentServiceFeeRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.benhsoan.port.outbound.time.ClockPort;

class CreateInvoiceServiceTest {

    @Test
    void createsOneServiceFeeLinePerCompletedClinicalItem() {
        PaymentRepository paymentRepository = mock(PaymentRepository.class);
        InvoiceRepository invoiceRepository = mock(InvoiceRepository.class);
        InvoiceCodeGenerator invoiceCodeGenerator = mock(InvoiceCodeGenerator.class);
        PaymentServiceFeeRepository paymentServiceFeeRepository = mock(PaymentServiceFeeRepository.class);
        UUID visitId = UUID.randomUUID();
        UUID clinicalItemId = UUID.randomUUID();
        Payment payment = Payment.restore(
                UUID.randomUUID(),
                visitId,
                new BigDecimal("100000"),
                new BigDecimal("150000"),
                new BigDecimal("95000"),
                new BigDecimal("345000"),
                new BigDecimal("345000"),
                PaymentMethod.CASH,
                PaymentStatus.RECORDED,
                UUID.randomUUID(),
                Instant.parse("2026-08-12T01:00:00Z"),
                Instant.parse("2026-08-12T01:00:00Z")
        );
        List<PaymentServiceFee> fees = List.of(
                PaymentServiceFee.create(
                        UUID.randomUUID(), payment.getId(), clinicalItemId, "Blood test",
                        new BigDecimal("95000"), Instant.parse("2026-08-12T01:00:00Z")
                )
        );
        when(paymentRepository.findByVisitId(visitId)).thenReturn(Optional.of(payment));
        when(invoiceRepository.findOriginalByVisitId(visitId)).thenReturn(Optional.empty());
        when(invoiceCodeGenerator.generate()).thenReturn("HD000009");
        when(paymentServiceFeeRepository.findAllByPaymentId(payment.getId())).thenReturn(fees);
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(invocation -> invocation.getArgument(0));
        CreateInvoiceService service = new CreateInvoiceService(
                paymentRepository,
                invoiceRepository,
                invoiceCodeGenerator,
                authorizedCurrentUser(),
                fixedClock(),
                mock(AuditLogRepository.class),
                new InvoiceResultMapper(),
                paymentServiceFeeRepository,
                mock(com.benhsoan.port.outbound.repository.billing.DiscountRequestRepository.class)
        );

        InvoiceResult result = service.create(new CreateInvoiceCommand(visitId, null));

        assertEquals(new BigDecimal("345000"), result.totalAmount());
        assertEquals(3, result.lines().size());
        assertEquals(InvoiceLineType.SERVICE_FEE, result.lines().get(2).lineType());
        assertEquals(clinicalItemId, result.lines().get(2).referenceId());
    }

    @Test
    void createsOriginalInvoiceFromVisitPayment() {
        PaymentRepository paymentRepository = mock(PaymentRepository.class);
        InvoiceRepository invoiceRepository = mock(InvoiceRepository.class);
        InvoiceCodeGenerator invoiceCodeGenerator = mock(InvoiceCodeGenerator.class);
        CurrentUserPort currentUserPort = authorizedCurrentUser();
        ClockPort clockPort = fixedClock();
        AuditLogRepository auditLogRepository = mock(AuditLogRepository.class);

        CreateInvoiceService service = new CreateInvoiceService(
                paymentRepository,
                invoiceRepository,
                invoiceCodeGenerator,
                currentUserPort,
                clockPort,
                auditLogRepository,
                new InvoiceResultMapper(),
                noServiceFees(),
                mock(com.benhsoan.port.outbound.repository.billing.DiscountRequestRepository.class)
        );

        UUID visitId = UUID.randomUUID();
        Payment payment = payment(visitId, PaymentStatus.RECORDED);
        when(paymentRepository.findByVisitId(visitId)).thenReturn(Optional.of(payment));
        when(invoiceRepository.findOriginalByVisitId(visitId)).thenReturn(Optional.empty());
        when(invoiceCodeGenerator.generate()).thenReturn("HD000010");
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(invocation -> invocation.getArgument(0));

        InvoiceResult result = service.create(new CreateInvoiceCommand(visitId, null));

        assertEquals("HD000010", result.invoiceCode());
        assertEquals(visitId, result.visitId());
        assertEquals(2, result.lines().size());
        assertEquals(new BigDecimal("250000"), result.totalAmount());
        verify(invoiceRepository).save(any(Invoice.class));
        verify(auditLogRepository).save(any());
    }

    @Test
    void createsOriginalInvoiceFromPaymentId() {
        PaymentRepository paymentRepository = mock(PaymentRepository.class);
        InvoiceRepository invoiceRepository = mock(InvoiceRepository.class);
        InvoiceCodeGenerator invoiceCodeGenerator = mock(InvoiceCodeGenerator.class);

        CreateInvoiceService service = new CreateInvoiceService(
                paymentRepository,
                invoiceRepository,
                invoiceCodeGenerator,
                authorizedCurrentUser(),
                fixedClock(),
                mock(AuditLogRepository.class),
                new InvoiceResultMapper(),
                noServiceFees(),
                mock(com.benhsoan.port.outbound.repository.billing.DiscountRequestRepository.class)
        );

        Payment payment = payment(UUID.randomUUID(), PaymentStatus.SUCCESS);
        when(paymentRepository.findById(payment.getId())).thenReturn(Optional.of(payment));
        when(invoiceRepository.findOriginalByVisitId(payment.getVisitId())).thenReturn(Optional.empty());
        when(invoiceCodeGenerator.generate()).thenReturn("HD000011");
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(invocation -> invocation.getArgument(0));

        InvoiceResult result = service.create(new CreateInvoiceCommand(null, payment.getId()));

        assertEquals(payment.getId(), result.paymentId());
        assertEquals("HD000011", result.invoiceCode());
    }

    @Test
    void createsOriginalInvoiceWithSingleNonZeroChargeLine() {
        PaymentRepository paymentRepository = mock(PaymentRepository.class);
        InvoiceRepository invoiceRepository = mock(InvoiceRepository.class);
        InvoiceCodeGenerator invoiceCodeGenerator = mock(InvoiceCodeGenerator.class);

        CreateInvoiceService service = new CreateInvoiceService(
                paymentRepository,
                invoiceRepository,
                invoiceCodeGenerator,
                authorizedCurrentUser(),
                fixedClock(),
                mock(AuditLogRepository.class),
                new InvoiceResultMapper(),
                noServiceFees(),
                mock(com.benhsoan.port.outbound.repository.billing.DiscountRequestRepository.class)
        );

        UUID visitId = UUID.randomUUID();
        Payment payment = Payment.restore(
                UUID.randomUUID(),
                visitId,
                new BigDecimal("100000"),
                BigDecimal.ZERO,
                new BigDecimal("100000"),
                new BigDecimal("100000"),
                PaymentMethod.CASH,
                PaymentStatus.SUCCESS,
                UUID.randomUUID(),
                Instant.parse("2026-08-12T01:00:00Z"),
                Instant.parse("2026-08-12T01:00:00Z")
        );

        when(paymentRepository.findByVisitId(visitId)).thenReturn(Optional.of(payment));
        when(invoiceRepository.findOriginalByVisitId(visitId)).thenReturn(Optional.empty());
        when(invoiceCodeGenerator.generate()).thenReturn("HD000012");
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(invocation -> invocation.getArgument(0));

        InvoiceResult result = service.create(new CreateInvoiceCommand(visitId, null));

        assertEquals(1, result.lines().size());
        assertEquals("EXAM_FEE", result.lines().get(0).lineType().name());
        assertEquals(new BigDecimal("100000"), result.totalAmount());
    }

    @Test
    void rejectsWhenPaymentDoesNotExist() {
        PaymentRepository paymentRepository = mock(PaymentRepository.class);
        when(paymentRepository.findByVisitId(any())).thenReturn(Optional.empty());

        CreateInvoiceService service = new CreateInvoiceService(
                paymentRepository,
                mock(InvoiceRepository.class),
                mock(InvoiceCodeGenerator.class),
                authorizedCurrentUser(),
                fixedClock(),
                mock(AuditLogRepository.class),
                new InvoiceResultMapper(),
                noServiceFees(),
                mock(com.benhsoan.port.outbound.repository.billing.DiscountRequestRepository.class)
        );

        assertThrows(
                PaymentNotFoundException.class,
                () -> service.create(new CreateInvoiceCommand(UUID.randomUUID(), null))
        );
    }

    @Test
    void rejectsWhenOriginalInvoiceAlreadyExists() {
        PaymentRepository paymentRepository = mock(PaymentRepository.class);
        InvoiceRepository invoiceRepository = mock(InvoiceRepository.class);
        Payment payment = payment(UUID.randomUUID(), PaymentStatus.RECORDED);

        when(paymentRepository.findById(payment.getId())).thenReturn(Optional.of(payment));
        when(invoiceRepository.findOriginalByVisitId(payment.getVisitId()))
                .thenReturn(Optional.of(mock(Invoice.class)));

        CreateInvoiceService service = new CreateInvoiceService(
                paymentRepository,
                invoiceRepository,
                mock(InvoiceCodeGenerator.class),
                authorizedCurrentUser(),
                fixedClock(),
                mock(AuditLogRepository.class),
                new InvoiceResultMapper(),
                noServiceFees(),
                mock(com.benhsoan.port.outbound.repository.billing.DiscountRequestRepository.class)
        );

        assertThrows(
                InvoiceAlreadyIssuedException.class,
                () -> service.create(new CreateInvoiceCommand(null, payment.getId()))
        );
    }

    @Test
    void mapsDuplicateInvoicePersistenceConflictToBusinessConflict() {
        PaymentRepository paymentRepository = mock(PaymentRepository.class);
        InvoiceRepository invoiceRepository = mock(InvoiceRepository.class);
        InvoiceCodeGenerator invoiceCodeGenerator = mock(InvoiceCodeGenerator.class);

        CreateInvoiceService service = new CreateInvoiceService(
                paymentRepository,
                invoiceRepository,
                invoiceCodeGenerator,
                authorizedCurrentUser(),
                fixedClock(),
                mock(AuditLogRepository.class),
                new InvoiceResultMapper(),
                noServiceFees(),
                mock(com.benhsoan.port.outbound.repository.billing.DiscountRequestRepository.class)
        );

        Payment payment = payment(UUID.randomUUID(), PaymentStatus.SUCCESS);
        when(paymentRepository.findById(payment.getId())).thenReturn(Optional.of(payment));
        when(invoiceRepository.findOriginalByVisitId(payment.getVisitId())).thenReturn(Optional.empty());
        when(invoiceCodeGenerator.generate()).thenReturn("HD000013");
        when(invoiceRepository.save(any(Invoice.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate key uk_invoices_payment"));

        assertThrows(
                InvoiceAlreadyIssuedException.class,
                () -> service.create(new CreateInvoiceCommand(null, payment.getId()))
        );
    }

    @Test
    void rethrowsUnrelatedPersistenceIntegrityError() {
        PaymentRepository paymentRepository = mock(PaymentRepository.class);
        InvoiceRepository invoiceRepository = mock(InvoiceRepository.class);
        InvoiceCodeGenerator invoiceCodeGenerator = mock(InvoiceCodeGenerator.class);

        CreateInvoiceService service = new CreateInvoiceService(
                paymentRepository,
                invoiceRepository,
                invoiceCodeGenerator,
                authorizedCurrentUser(),
                fixedClock(),
                mock(AuditLogRepository.class),
                new InvoiceResultMapper(),
                noServiceFees(),
                mock(com.benhsoan.port.outbound.repository.billing.DiscountRequestRepository.class)
        );

        Payment payment = payment(UUID.randomUUID(), PaymentStatus.SUCCESS);
        when(paymentRepository.findById(payment.getId())).thenReturn(Optional.of(payment));
        when(invoiceRepository.findOriginalByVisitId(payment.getVisitId())).thenReturn(Optional.empty());
        when(invoiceCodeGenerator.generate()).thenReturn("HD000014");
        when(invoiceRepository.save(any(Invoice.class)))
                .thenThrow(new DataIntegrityViolationException("fk_invoices_created_by"));

        assertThrows(
                DataIntegrityViolationException.class,
                () -> service.create(new CreateInvoiceCommand(null, payment.getId()))
        );
    }

    @Test
    void rejectsInvalidCommandWithoutVisitOrPaymentId() {
        CreateInvoiceService service = new CreateInvoiceService(
                mock(PaymentRepository.class),
                mock(InvoiceRepository.class),
                mock(InvoiceCodeGenerator.class),
                authorizedCurrentUser(),
                fixedClock(),
                mock(AuditLogRepository.class),
                new InvoiceResultMapper(),
                noServiceFees(),
                mock(com.benhsoan.port.outbound.repository.billing.DiscountRequestRepository.class)
        );

        assertThrows(
                ValidationException.class,
                () -> service.create(new CreateInvoiceCommand(null, null))
        );
    }

    @Test
    void rejectsWhenVisitIdDoesNotMatchPaymentId() {
        PaymentRepository paymentRepository = mock(PaymentRepository.class);
        InvoiceRepository invoiceRepository = mock(InvoiceRepository.class);
        InvoiceCodeGenerator invoiceCodeGenerator = mock(InvoiceCodeGenerator.class);

        CreateInvoiceService service = new CreateInvoiceService(
                paymentRepository,
                invoiceRepository,
                invoiceCodeGenerator,
                authorizedCurrentUser(),
                fixedClock(),
                mock(AuditLogRepository.class),
                new InvoiceResultMapper(),
                noServiceFees(),
                mock(com.benhsoan.port.outbound.repository.billing.DiscountRequestRepository.class)
        );

        UUID paymentVisitId = UUID.randomUUID();
        Payment payment = payment(paymentVisitId, PaymentStatus.SUCCESS);
        when(paymentRepository.findById(payment.getId())).thenReturn(Optional.of(payment));

        assertThrows(
                ValidationException.class,
                () -> service.create(new CreateInvoiceCommand(UUID.randomUUID(), payment.getId()))
        );
    }

    @Test
    void rejectsUnauthorizedActor() {
        CurrentUserPort currentUserPort = mock(CurrentUserPort.class);
        when(currentUserPort.hasRole("ADMIN")).thenReturn(false);
        when(currentUserPort.hasRole("RECEPTIONIST")).thenReturn(false);

        CreateInvoiceService service = new CreateInvoiceService(
                mock(PaymentRepository.class),
                mock(InvoiceRepository.class),
                mock(InvoiceCodeGenerator.class),
                currentUserPort,
                fixedClock(),
                mock(AuditLogRepository.class),
                new InvoiceResultMapper(),
                noServiceFees(),
                mock(com.benhsoan.port.outbound.repository.billing.DiscountRequestRepository.class)
        );

        assertThrows(
                AccessDeniedException.class,
                () -> service.create(new CreateInvoiceCommand(UUID.randomUUID(), null))
        );
    }

    @Test
    void rejectsCreateInvoiceWhenDiscountApprovalIsPending() {
        PaymentRepository paymentRepository = mock(PaymentRepository.class);
        com.benhsoan.port.outbound.repository.billing.DiscountRequestRepository discountRequestRepository =
                mock(com.benhsoan.port.outbound.repository.billing.DiscountRequestRepository.class);
        UUID visitId = UUID.randomUUID();
        Payment payment = payment(visitId, PaymentStatus.RECORDED);
        when(paymentRepository.findByVisitId(visitId)).thenReturn(Optional.of(payment));
        when(discountRequestRepository.existsByVisitIdAndStatus(visitId, com.benhsoan.domain.billing.enums.DiscountRequestStatus.PENDING))
                .thenReturn(true);

        CreateInvoiceService service = new CreateInvoiceService(
                paymentRepository,
                mock(InvoiceRepository.class),
                mock(InvoiceCodeGenerator.class),
                authorizedCurrentUser(),
                fixedClock(),
                mock(AuditLogRepository.class),
                new InvoiceResultMapper(),
                noServiceFees(),
                discountRequestRepository
        );

        assertThrows(
                com.benhsoan.domain.billing.exception.PendingDiscountApprovalException.class,
                () -> service.create(new CreateInvoiceCommand(visitId, null))
        );
    }

    @Test
    void createsOriginalInvoiceWithApprovedDiscountLine() {
        PaymentRepository paymentRepository = mock(PaymentRepository.class);
        InvoiceRepository invoiceRepository = mock(InvoiceRepository.class);
        InvoiceCodeGenerator invoiceCodeGenerator = mock(InvoiceCodeGenerator.class);
        com.benhsoan.port.outbound.repository.billing.DiscountRequestRepository discountRequestRepository =
                mock(com.benhsoan.port.outbound.repository.billing.DiscountRequestRepository.class);
        UUID visitId = UUID.randomUUID();
        UUID discountRequestId = UUID.randomUUID();

        Payment payment = Payment.restore(
                UUID.randomUUID(),
                visitId,
                new BigDecimal("100000"),
                new BigDecimal("150000"),
                BigDecimal.ZERO,
                new BigDecimal("50000"),
                discountRequestId,
                new BigDecimal("250000"),
                new BigDecimal("200000"),
                PaymentMethod.CASH,
                PaymentStatus.RECORDED,
                UUID.randomUUID(),
                Instant.parse("2026-08-12T01:00:00Z"),
                null,
                null,
                null,
                Instant.parse("2026-08-12T01:00:00Z")
        );

        when(paymentRepository.findByVisitId(visitId)).thenReturn(Optional.of(payment));
        when(invoiceRepository.findOriginalByVisitId(visitId)).thenReturn(Optional.empty());
        when(invoiceCodeGenerator.generate()).thenReturn("HD000099");
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(inv -> inv.getArgument(0));

        com.benhsoan.domain.billing.DiscountRequest request = com.benhsoan.domain.billing.DiscountRequest.restore(
                discountRequestId,
                visitId,
                com.benhsoan.domain.billing.enums.DiscountType.PERCENTAGE,
                new BigDecimal("20"),
                new BigDecimal("250000"),
                new BigDecimal("50000"),
                new BigDecimal("200000"),
                "Lý do",
                com.benhsoan.domain.billing.enums.DiscountRequestStatus.APPROVED,
                UUID.randomUUID(),
                Instant.now(),
                UUID.randomUUID(),
                Instant.now(),
                null,
                null,
                null,
                null
        );
        when(discountRequestRepository.findById(discountRequestId)).thenReturn(Optional.of(request));

        CreateInvoiceService service = new CreateInvoiceService(
                paymentRepository,
                invoiceRepository,
                invoiceCodeGenerator,
                authorizedCurrentUser(),
                fixedClock(),
                mock(AuditLogRepository.class),
                new InvoiceResultMapper(),
                noServiceFees(),
                discountRequestRepository
        );

        InvoiceResult result = service.create(new CreateInvoiceCommand(visitId, null));

        assertEquals(new BigDecimal("200000"), result.totalAmount());
        assertEquals(3, result.lines().size());
        assertEquals(InvoiceLineType.DISCOUNT, result.lines().get(2).lineType());
        assertEquals(new BigDecimal("-50000"), result.lines().get(2).amount());
        assertEquals(new BigDecimal("50000"), result.discountAmount());
        assertEquals(discountRequestId, result.discountRequestId());
    }

    private static CurrentUserPort authorizedCurrentUser() {
        CurrentUserPort currentUserPort = mock(CurrentUserPort.class);
        when(currentUserPort.hasRole("ADMIN")).thenReturn(false);
        when(currentUserPort.hasRole("RECEPTIONIST")).thenReturn(true);
        when(currentUserPort.getCurrentUserId()).thenReturn(UUID.randomUUID());
        return currentUserPort;
    }

    private static PaymentServiceFeeRepository noServiceFees() {
        PaymentServiceFeeRepository repository = mock(PaymentServiceFeeRepository.class);
        when(repository.findAllByPaymentId(any())).thenReturn(List.of());
        return repository;
    }

    private static ClockPort fixedClock() {
        ClockPort clockPort = mock(ClockPort.class);
        when(clockPort.now()).thenReturn(Instant.parse("2026-08-12T02:00:00Z"));
        return clockPort;
    }

    private static Payment payment(UUID visitId, PaymentStatus status) {
        return Payment.restore(
                UUID.randomUUID(),
                visitId,
                new BigDecimal("100000"),
                new BigDecimal("150000"),
                new BigDecimal("250000"),
                new BigDecimal("250000"),
                PaymentMethod.CASH,
                status,
                UUID.randomUUID(),
                Instant.parse("2026-08-12T01:00:00Z"),
                Instant.parse("2026-08-12T01:00:00Z")
        );
    }
}

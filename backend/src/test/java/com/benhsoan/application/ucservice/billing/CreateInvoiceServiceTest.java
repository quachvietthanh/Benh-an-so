package com.benhsoan.application.ucservice.billing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

import com.benhsoan.domain.billing.Invoice;
import com.benhsoan.domain.billing.Payment;
import com.benhsoan.domain.billing.enums.PaymentMethod;
import com.benhsoan.domain.billing.enums.PaymentStatus;
import com.benhsoan.domain.billing.exception.InvoiceAlreadyIssuedException;
import com.benhsoan.domain.billing.exception.PaymentNotFoundException;
import com.benhsoan.domain.shared.exception.ValidationException;
import com.benhsoan.port.dto.command.billing.CreateInvoiceCommand;
import com.benhsoan.port.dto.result.InvoiceResult;
import com.benhsoan.port.outbound.generator.InvoiceCodeGenerator;
import com.benhsoan.port.outbound.repository.audit.AuditLogRepository;
import com.benhsoan.port.outbound.repository.billing.InvoiceRepository;
import com.benhsoan.port.outbound.repository.billing.PaymentRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.benhsoan.port.outbound.time.ClockPort;

class CreateInvoiceServiceTest {

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
                new InvoiceResultMapper()
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
                new InvoiceResultMapper()
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
                new InvoiceResultMapper()
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
                new InvoiceResultMapper()
        );

        assertThrows(
                InvoiceAlreadyIssuedException.class,
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
                new InvoiceResultMapper()
        );

        assertThrows(
                ValidationException.class,
                () -> service.create(new CreateInvoiceCommand(null, null))
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
                new InvoiceResultMapper()
        );

        assertThrows(
                AccessDeniedException.class,
                () -> service.create(new CreateInvoiceCommand(UUID.randomUUID(), null))
        );
    }

    private static CurrentUserPort authorizedCurrentUser() {
        CurrentUserPort currentUserPort = mock(CurrentUserPort.class);
        when(currentUserPort.hasRole("ADMIN")).thenReturn(false);
        when(currentUserPort.hasRole("RECEPTIONIST")).thenReturn(true);
        when(currentUserPort.getCurrentUserId()).thenReturn(UUID.randomUUID());
        return currentUserPort;
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

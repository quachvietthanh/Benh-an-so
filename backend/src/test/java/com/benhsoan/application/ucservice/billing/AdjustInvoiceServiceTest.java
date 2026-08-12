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

import com.benhsoan.domain.billing.Invoice;
import com.benhsoan.domain.billing.enums.InvoiceLineType;
import com.benhsoan.domain.billing.enums.InvoiceType;
import com.benhsoan.domain.billing.exception.InvoiceAdjustmentReasonRequiredException;
import com.benhsoan.domain.billing.exception.InvoiceNotFoundException;
import com.benhsoan.domain.billing.exception.InvoiceUnauthorizedAdjustmentException;
import com.benhsoan.domain.shared.exception.ValidationException;
import com.benhsoan.port.dto.command.billing.AdjustInvoiceCommand;
import com.benhsoan.port.dto.command.billing.AdjustmentInvoiceLineCommand;
import com.benhsoan.port.dto.result.InvoiceResult;
import com.benhsoan.port.outbound.generator.AdjustmentInvoiceCodeGenerator;
import com.benhsoan.port.outbound.repository.audit.AuditLogRepository;
import com.benhsoan.port.outbound.repository.billing.InvoiceRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.benhsoan.port.outbound.time.ClockPort;

class AdjustInvoiceServiceTest {

    @Test
    void createsAdjustmentInvoiceLinkedToOriginalInvoice() {
        InvoiceRepository invoiceRepository = mock(InvoiceRepository.class);
        AdjustmentInvoiceCodeGenerator codeGenerator = mock(AdjustmentInvoiceCodeGenerator.class);
        CurrentUserPort currentUserPort = adminCurrentUser();
        ClockPort clockPort = fixedClock();
        AuditLogRepository auditLogRepository = mock(AuditLogRepository.class);

        AdjustInvoiceService service = new AdjustInvoiceService(
                invoiceRepository,
                codeGenerator,
                currentUserPort,
                clockPort,
                auditLogRepository,
                new InvoiceResultMapper()
        );

        Invoice originalInvoice = originalInvoice();
        when(invoiceRepository.findById(originalInvoice.getId())).thenReturn(Optional.of(originalInvoice));
        when(codeGenerator.generate()).thenReturn("HDDC000010");
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(invocation -> invocation.getArgument(0));

        InvoiceResult result = service.adjust(new AdjustInvoiceCommand(
                originalInvoice.getId(),
                "Dieu chinh giam tien thuoc",
                List.of(
                        new AdjustmentInvoiceLineCommand(
                                "Dieu chinh dong 1",
                                null,
                                1,
                                new BigDecimal("-20000")
                        ),
                        new AdjustmentInvoiceLineCommand(
                                "Dieu chinh dong 2",
                                UUID.randomUUID(),
                                1,
                                new BigDecimal("5000")
                        )
                )
        ));

        assertEquals("HDDC000010", result.invoiceCode());
        assertEquals(InvoiceType.ADJUSTMENT, result.type());
        assertEquals(originalInvoice.getId(), result.originalInvoiceId());
        assertEquals(new BigDecimal("-15000"), result.totalAmount());
        assertEquals(2, result.lines().size());
        assertEquals(InvoiceLineType.ADJUSTMENT, result.lines().get(0).lineType());
        verify(invoiceRepository).save(any(Invoice.class));
        verify(auditLogRepository).save(any());
    }

    @Test
    void rejectsWhenOriginalInvoiceDoesNotExist() {
        InvoiceRepository invoiceRepository = mock(InvoiceRepository.class);
        when(invoiceRepository.findById(any())).thenReturn(Optional.empty());

        AdjustInvoiceService service = new AdjustInvoiceService(
                invoiceRepository,
                mock(AdjustmentInvoiceCodeGenerator.class),
                adminCurrentUser(),
                fixedClock(),
                mock(AuditLogRepository.class),
                new InvoiceResultMapper()
        );

        assertThrows(
                InvoiceNotFoundException.class,
                () -> service.adjust(command(UUID.randomUUID(), "Ly do"))
        );
    }

    @Test
    void rejectsWhenInvoiceIsNotOriginal() {
        InvoiceRepository invoiceRepository = mock(InvoiceRepository.class);
        UUID adjustmentInvoiceId = UUID.randomUUID();
        Invoice adjustmentInvoice = Invoice.restore(
                adjustmentInvoiceId,
                "HDDC000001",
                UUID.randomUUID(),
                null,
                InvoiceType.ADJUSTMENT,
                UUID.randomUUID(),
                "Da dieu chinh",
                new BigDecimal("-10000"),
                UUID.randomUUID(),
                Instant.parse("2026-08-12T03:00:00Z"),
                List.of(com.benhsoan.domain.billing.InvoiceLine.create(
                        UUID.randomUUID(),
                        adjustmentInvoiceId,
                        InvoiceLineType.ADJUSTMENT,
                        "Dieu chinh",
                        UUID.randomUUID(),
                        1,
                        new BigDecimal("-10000"),
                        new BigDecimal("-10000"),
                        Instant.parse("2026-08-12T03:00:00Z")
                ))
        );
        when(invoiceRepository.findById(adjustmentInvoice.getId())).thenReturn(Optional.of(adjustmentInvoice));

        AdjustInvoiceService service = new AdjustInvoiceService(
                invoiceRepository,
                mock(AdjustmentInvoiceCodeGenerator.class),
                adminCurrentUser(),
                fixedClock(),
                mock(AuditLogRepository.class),
                new InvoiceResultMapper()
        );

        assertThrows(
                ValidationException.class,
                () -> service.adjust(command(adjustmentInvoice.getId(), "Ly do"))
        );
    }

    @Test
    void rejectsWhenReasonIsBlank() {
        InvoiceRepository invoiceRepository = mock(InvoiceRepository.class);
        Invoice originalInvoice = originalInvoice();
        when(invoiceRepository.findById(originalInvoice.getId())).thenReturn(Optional.of(originalInvoice));

        AdjustInvoiceService service = new AdjustInvoiceService(
                invoiceRepository,
                mock(AdjustmentInvoiceCodeGenerator.class),
                adminCurrentUser(),
                fixedClock(),
                mock(AuditLogRepository.class),
                new InvoiceResultMapper()
        );

        assertThrows(
                InvoiceAdjustmentReasonRequiredException.class,
                () -> service.adjust(command(originalInvoice.getId(), " "))
        );
    }

    @Test
    void rejectsUnauthorizedActor() {
        CurrentUserPort currentUserPort = mock(CurrentUserPort.class);
        when(currentUserPort.hasRole("ADMIN")).thenReturn(false);
        when(currentUserPort.getCurrentUserId()).thenReturn(UUID.randomUUID());

        InvoiceRepository invoiceRepository = mock(InvoiceRepository.class);
        Invoice originalInvoice = originalInvoice();
        when(invoiceRepository.findById(originalInvoice.getId())).thenReturn(Optional.of(originalInvoice));

        AdjustInvoiceService service = new AdjustInvoiceService(
                invoiceRepository,
                mock(AdjustmentInvoiceCodeGenerator.class),
                currentUserPort,
                fixedClock(),
                mock(AuditLogRepository.class),
                new InvoiceResultMapper()
        );

        assertThrows(
                InvoiceUnauthorizedAdjustmentException.class,
                () -> service.adjust(command(originalInvoice.getId(), "Ly do"))
        );
    }

    @Test
    void rejectsWhenNoAdjustmentLines() {
        AdjustInvoiceService service = new AdjustInvoiceService(
                mock(InvoiceRepository.class),
                mock(AdjustmentInvoiceCodeGenerator.class),
                adminCurrentUser(),
                fixedClock(),
                mock(AuditLogRepository.class),
                new InvoiceResultMapper()
        );

        assertThrows(
                ValidationException.class,
                () -> service.adjust(new AdjustInvoiceCommand(UUID.randomUUID(), "Ly do", List.of()))
        );
    }

    @Test
    void rejectsWhenAdjustmentUnitPriceIsZero() {
        InvoiceRepository invoiceRepository = mock(InvoiceRepository.class);
        Invoice originalInvoice = originalInvoice();
        when(invoiceRepository.findById(originalInvoice.getId())).thenReturn(Optional.of(originalInvoice));

        AdjustInvoiceService service = new AdjustInvoiceService(
                invoiceRepository,
                mock(AdjustmentInvoiceCodeGenerator.class),
                adminCurrentUser(),
                fixedClock(),
                mock(AuditLogRepository.class),
                new InvoiceResultMapper()
        );

        assertThrows(
                ValidationException.class,
                () -> service.adjust(new AdjustInvoiceCommand(
                        originalInvoice.getId(),
                        "Ly do",
                        List.of(new AdjustmentInvoiceLineCommand(
                                "Dong loi",
                                null,
                                1,
                                BigDecimal.ZERO
                        ))
                ))
        );
    }

    private static AdjustInvoiceCommand command(UUID originalInvoiceId, String reason) {
        return new AdjustInvoiceCommand(
                originalInvoiceId,
                reason,
                List.of(new AdjustmentInvoiceLineCommand(
                        "Dieu chinh giam phi",
                        null,
                        1,
                        new BigDecimal("-10000")
                ))
        );
    }

    private static CurrentUserPort adminCurrentUser() {
        CurrentUserPort currentUserPort = mock(CurrentUserPort.class);
        when(currentUserPort.hasRole("ADMIN")).thenReturn(true);
        when(currentUserPort.getCurrentUserId()).thenReturn(UUID.randomUUID());
        return currentUserPort;
    }

    private static ClockPort fixedClock() {
        ClockPort clockPort = mock(ClockPort.class);
        when(clockPort.now()).thenReturn(Instant.parse("2026-08-12T04:00:00Z"));
        return clockPort;
    }

    private static Invoice originalInvoice() {
        UUID invoiceId = UUID.randomUUID();
        UUID visitId = UUID.randomUUID();
        UUID paymentId = UUID.randomUUID();
        Instant now = Instant.parse("2026-08-12T03:00:00Z");

        return Invoice.restore(
                invoiceId,
                "HD000001",
                visitId,
                paymentId,
                InvoiceType.ORIGINAL,
                null,
                null,
                new BigDecimal("250000"),
                UUID.randomUUID(),
                now,
                List.of(
                        com.benhsoan.domain.billing.InvoiceLine.create(
                                UUID.randomUUID(),
                                invoiceId,
                                InvoiceLineType.EXAM_FEE,
                                "Phi kham",
                                visitId,
                                1,
                                new BigDecimal("100000"),
                                new BigDecimal("100000"),
                                now
                        ),
                        com.benhsoan.domain.billing.InvoiceLine.create(
                                UUID.randomUUID(),
                                invoiceId,
                                InvoiceLineType.MEDICINE_FEE,
                                "Tien thuoc",
                                paymentId,
                                1,
                                new BigDecimal("150000"),
                                new BigDecimal("150000"),
                                now
                        )
                )
        );
    }
}

package com.benhsoan.application.ucservice.billing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.benhsoan.domain.auth.User;
import com.benhsoan.domain.billing.Invoice;
import com.benhsoan.domain.billing.InvoiceLine;
import com.benhsoan.domain.billing.Payment;
import com.benhsoan.domain.billing.PaymentMethodItem;
import com.benhsoan.domain.billing.enums.InvoiceLineType;
import com.benhsoan.domain.billing.enums.InvoiceType;
import com.benhsoan.domain.billing.enums.PaymentMethod;
import com.benhsoan.domain.billing.enums.PaymentStatus;
import com.benhsoan.domain.billing.exception.InvoiceNotFoundException;
import com.benhsoan.port.dto.result.InvoiceResult;
import com.benhsoan.port.outbound.repository.auth.UserRepository;
import com.benhsoan.port.outbound.repository.billing.InvoiceRepository;
import com.benhsoan.port.outbound.repository.billing.PaymentRepository;

class GetInvoiceByIdServiceTest {

    @Test
    @DisplayName("getById returns invoice with payment breakdown and collector name (TC-04)")
    void returnsInvoiceWithPaymentBreakdownAndCollectorName() {
        InvoiceRepository invoiceRepository = mock(InvoiceRepository.class);
        PaymentRepository paymentRepository = mock(PaymentRepository.class);
        UserRepository userRepository = mock(UserRepository.class);
        InvoiceResultMapper resultMapper = new InvoiceResultMapper();

        GetInvoiceByIdService service = new GetInvoiceByIdService(
                invoiceRepository,
                paymentRepository,
                userRepository,
                resultMapper
        );

        UUID invoiceId = UUID.randomUUID();
        UUID visitId = UUID.randomUUID();
        UUID paymentId = UUID.randomUUID();
        UUID collectorId = UUID.randomUUID();
        Instant paidAt = Instant.parse("2026-08-12T02:00:00Z");

        Invoice invoice = Invoice.restore(
                invoiceId,
                "HD000010",
                visitId,
                paymentId,
                InvoiceType.ORIGINAL,
                null,
                null,
                new BigDecimal("250000"),
                collectorId,
                paidAt,
                0,
                null,
                List.of(
                        InvoiceLine.create(
                                UUID.randomUUID(),
                                invoiceId,
                                InvoiceLineType.EXAM_FEE,
                                "Phi kham",
                                visitId,
                                1,
                                new BigDecimal("100000"),
                                new BigDecimal("100000"),
                                paidAt
                        ),
                        InvoiceLine.create(
                                UUID.randomUUID(),
                                invoiceId,
                                InvoiceLineType.MEDICINE_FEE,
                                "Tien thuoc",
                                paymentId,
                                1,
                                new BigDecimal("150000"),
                                new BigDecimal("150000"),
                                paidAt
                        )
                )
        );

        List<PaymentMethodItem> paymentItems = List.of(
                PaymentMethodItem.restore(
                        UUID.randomUUID(),
                        paymentId,
                        PaymentMethod.CASH,
                        new BigDecimal("100000"),
                        null,
                        paidAt
                ),
                PaymentMethodItem.restore(
                        UUID.randomUUID(),
                        paymentId,
                        PaymentMethod.BANK_TRANSFER,
                        new BigDecimal("150000"),
                        "TXN123456",
                        paidAt
                )
        );

        Payment payment = Payment.restore(
                paymentId,
                visitId,
                new BigDecimal("100000"),
                new BigDecimal("150000"),
                BigDecimal.ZERO,
                new BigDecimal("250000"),
                new BigDecimal("250000"),
                PaymentMethod.MULTIPLE,
                PaymentStatus.RECORDED,
                collectorId,
                paidAt,
                null,
                null,
                null,
                paidAt,
                paymentItems
        );

        User collector = User.restore(
                collectorId,
                "thungan",
                "hash",
                "Nguyen Van Thu Ngan",
                "thungan@clinic.vn",
                "0987654321",
                UUID.randomUUID(),
                true,
                false,
                null,
                null,
                paidAt
        );

        when(invoiceRepository.findById(invoiceId)).thenReturn(Optional.of(invoice));
        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment));
        when(userRepository.findById(collectorId)).thenReturn(Optional.of(collector));

        InvoiceResult result = service.getById(invoiceId);

        assertNotNull(result);
        assertEquals(invoiceId, result.id());
        assertNotNull(result.payment());
        assertEquals("Nguyen Van Thu Ngan", result.payment().collectorName());
        assertEquals(PaymentMethod.MULTIPLE, result.payment().paymentMethod());
        assertEquals(2, result.payment().paymentMethods().size());
        assertEquals(PaymentMethod.CASH, result.payment().paymentMethods().get(0).paymentMethod());
        assertEquals(new BigDecimal("100000"), result.payment().paymentMethods().get(0).amount());
        assertEquals(PaymentMethod.BANK_TRANSFER, result.payment().paymentMethods().get(1).paymentMethod());
        assertEquals("TXN123456", result.payment().paymentMethods().get(1).referenceNumber());
    }

    @Test
    @DisplayName("getById returns invoice without payment when paymentId is null")
    void returnsInvoiceWithoutPaymentWhenPaymentIdIsNull() {
        InvoiceRepository invoiceRepository = mock(InvoiceRepository.class);
        PaymentRepository paymentRepository = mock(PaymentRepository.class);
        UserRepository userRepository = mock(UserRepository.class);
        InvoiceResultMapper resultMapper = new InvoiceResultMapper();

        GetInvoiceByIdService service = new GetInvoiceByIdService(
                invoiceRepository,
                paymentRepository,
                userRepository,
                resultMapper
        );

        UUID invoiceId = UUID.randomUUID();
        UUID visitId = UUID.randomUUID();
        Instant now = Instant.parse("2026-08-12T03:00:00Z");

        Invoice invoice = Invoice.restore(
                invoiceId,
                "HDDC000002",
                visitId,
                null,
                InvoiceType.ADJUSTMENT,
                UUID.randomUUID(),
                "Dieu chinh",
                new BigDecimal("-20000"),
                UUID.randomUUID(),
                now,
                0,
                null,
                List.of(
                        InvoiceLine.create(
                                UUID.randomUUID(),
                                invoiceId,
                                InvoiceLineType.ADJUSTMENT,
                                "Giam tien",
                                null,
                                1,
                                new BigDecimal("-20000"),
                                new BigDecimal("-20000"),
                                now
                        )
                )
        );

        when(invoiceRepository.findById(invoiceId)).thenReturn(Optional.of(invoice));

        InvoiceResult result = service.getById(invoiceId);

        assertNotNull(result);
        assertNull(result.payment());
    }

    @Test
    @DisplayName("getById throws InvoiceNotFoundException when invoice not found")
    void throwsInvoiceNotFoundExceptionWhenNotFound() {
        InvoiceRepository invoiceRepository = mock(InvoiceRepository.class);
        GetInvoiceByIdService service = new GetInvoiceByIdService(
                invoiceRepository,
                mock(PaymentRepository.class),
                mock(UserRepository.class),
                new InvoiceResultMapper()
        );

        UUID invoiceId = UUID.randomUUID();
        when(invoiceRepository.findById(invoiceId)).thenReturn(Optional.empty());

        assertThrows(InvoiceNotFoundException.class, () -> service.getById(invoiceId));
    }
}

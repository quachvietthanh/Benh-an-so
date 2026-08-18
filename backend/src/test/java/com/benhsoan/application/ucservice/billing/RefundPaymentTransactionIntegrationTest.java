package com.benhsoan.application.ucservice.billing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.domain.billing.Invoice;
import com.benhsoan.domain.auditlog.AuditLog;
import com.benhsoan.domain.billing.enums.InvoiceLineType;
import com.benhsoan.domain.billing.enums.InvoiceType;
import com.benhsoan.domain.billing.enums.PaymentMethod;
import com.benhsoan.domain.billing.enums.PaymentStatus;
import com.benhsoan.domain.billing.exception.PaymentNotAllowedException;
import com.benhsoan.persistence.adapterRepository.auditlog.AuditLogRepositoryAdapter;
import com.benhsoan.persistence.adapterRepository.billing.InvoiceRepositoryAdapter;
import com.benhsoan.persistence.adapterRepository.billing.PaymentRepositoryAdapter;
import com.benhsoan.persistence.entity.billing.InvoiceEntity;
import com.benhsoan.persistence.entity.billing.InvoiceLineEntity;
import com.benhsoan.persistence.entity.billing.PaymentEntity;
import com.benhsoan.persistence.jpaRepository.auditlog.JpaAuditLogRepository;
import com.benhsoan.persistence.jpaRepository.billing.JpaInvoiceLineRepository;
import com.benhsoan.persistence.jpaRepository.billing.JpaInvoiceRepository;
import com.benhsoan.persistence.jpaRepository.billing.JpaPaymentRepository;
import com.benhsoan.persistence.mapper.auditlog.AuditLogPersistenceMapper;
import com.benhsoan.persistence.mapper.billing.InvoiceLinePersistenceMapper;
import com.benhsoan.persistence.mapper.billing.InvoicePersistenceMapper;
import com.benhsoan.persistence.mapper.billing.PaymentPersistenceMapper;
import com.benhsoan.port.dto.command.billing.RefundPaymentCommand;
import com.benhsoan.port.dto.result.RefundPaymentResult;
import com.benhsoan.port.outbound.generator.AdjustmentInvoiceCodeGenerator;
import com.benhsoan.port.outbound.repository.medicalrecord.MedicalRecordRepository;
import com.benhsoan.port.outbound.repository.prescription.PrescriptionRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.benhsoan.port.outbound.time.ClockPort;

@DataJpaTest(properties = {
        "spring.flyway.enabled=false",
        "spring.sql.init.mode=never",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@Import({
        RefundPaymentService.class,
        InvoiceResultMapper.class,
        PaymentRepositoryAdapter.class,
        InvoiceRepositoryAdapter.class,
        AuditLogRepositoryAdapter.class,
        PaymentPersistenceMapper.class,
        InvoicePersistenceMapper.class,
        InvoiceLinePersistenceMapper.class,
        AuditLogPersistenceMapper.class
})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class RefundPaymentTransactionIntegrationTest {

    private static final Instant PAID_AT = Instant.parse("2026-08-17T03:00:00Z");
    private static final Instant REFUNDED_AT = Instant.parse("2026-08-18T04:00:00Z");
    private static final UUID ACTOR_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa7");

    @Autowired private RefundPaymentService service;
    @Autowired private JpaPaymentRepository paymentRepository;
    @Autowired private JpaInvoiceRepository invoiceRepository;
    @Autowired private JpaInvoiceLineRepository invoiceLineRepository;
    @Autowired private JpaAuditLogRepository auditLogRepository;

    @MockitoSpyBean private InvoiceRepositoryAdapter invoiceRepositoryAdapter;
    @MockitoSpyBean private AuditLogRepositoryAdapter auditLogRepositoryAdapter;
    @MockitoBean private MedicalRecordRepository medicalRecordRepository;
    @MockitoBean private PrescriptionRepository prescriptionRepository;
    @MockitoBean private AdjustmentInvoiceCodeGenerator adjustmentInvoiceCodeGenerator;
    @MockitoBean private CurrentUserPort currentUserPort;
    @MockitoBean private ClockPort clockPort;

    @BeforeEach
    void setUp() {
        when(currentUserPort.hasRole("MANAGER")).thenReturn(true);
        when(currentUserPort.getCurrentUserId()).thenReturn(ACTOR_ID);
        when(clockPort.now()).thenReturn(REFUNDED_AT);
        when(medicalRecordRepository.findByVisitId(any())).thenReturn(Optional.empty());
        when(adjustmentInvoiceCodeGenerator.generate())
                .thenReturn("HDDC-" + UUID.randomUUID().toString().substring(0, 8));
    }

    @Test
    void rollsBackPaymentWhenAdjustmentSaveFails() {
        RefundFixture fixture = seedRefundablePayment();
        long invoiceCountBefore = invoiceRepository.count();
        long auditCountBefore = auditLogRepository.count();
        doThrow(new IllegalStateException("Simulated adjustment persistence failure"))
                .when(invoiceRepositoryAdapter).save(any(Invoice.class));

        assertThrows(
                IllegalStateException.class,
                () -> service.refund(command(fixture.paymentId()))
        );

        PaymentEntity payment = paymentRepository.findById(fixture.paymentId()).orElseThrow();
        assertEquals(PaymentStatus.SUCCESS, payment.getStatus());
        assertNull(payment.getRefundReason());
        assertNull(payment.getRefundedBy());
        assertNull(payment.getRefundedAt());
        assertEquals(invoiceCountBefore, invoiceRepository.count());
        assertEquals(auditCountBefore, auditLogRepository.count());
    }

    @Test
    void rollsBackPaymentAndAdjustmentWhenAuditSaveFails() {
        RefundFixture fixture = seedRefundablePayment();
        long invoiceCountBefore = invoiceRepository.count();
        long auditCountBefore = auditLogRepository.count();
        doThrow(new IllegalStateException("Simulated audit persistence failure"))
                .when(auditLogRepositoryAdapter).save(any(AuditLog.class));

        assertThrows(
                IllegalStateException.class,
                () -> service.refund(command(fixture.paymentId()))
        );

        PaymentEntity payment = paymentRepository.findById(fixture.paymentId()).orElseThrow();
        assertEquals(PaymentStatus.SUCCESS, payment.getStatus());
        assertNull(payment.getRefundedAt());
        assertEquals(invoiceCountBefore, invoiceRepository.count());
        assertEquals(auditCountBefore, auditLogRepository.count());
    }

    @Test
    void allowsOnlyOneOfTwoConcurrentRefunds() throws Exception {
        RefundFixture fixture = seedRefundablePayment();
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);

        try {
            Future<Object> first = executor.submit(
                    () -> attemptRefund(fixture.paymentId(), ready, start)
            );
            Future<Object> second = executor.submit(
                    () -> attemptRefund(fixture.paymentId(), ready, start)
            );
            ready.await(5, TimeUnit.SECONDS);
            start.countDown();

            List<Object> outcomes = List.of(
                    first.get(10, TimeUnit.SECONDS),
                    second.get(10, TimeUnit.SECONDS)
            );
            long successes = outcomes.stream()
                    .filter(RefundPaymentResult.class::isInstance)
                    .count();
            long conflicts = outcomes.stream()
                    .filter(PaymentNotAllowedException.class::isInstance)
                    .count();

            assertEquals(1, successes);
            assertEquals(1, conflicts);
            outcomes.stream()
                    .filter(outcome -> !(outcome instanceof RefundPaymentResult))
                    .forEach(outcome -> assertInstanceOf(PaymentNotAllowedException.class, outcome));
        } finally {
            executor.shutdownNow();
        }

        PaymentEntity payment = paymentRepository.findById(fixture.paymentId()).orElseThrow();
        assertEquals(PaymentStatus.REFUNDED, payment.getStatus());
        assertEquals(REFUNDED_AT, payment.getRefundedAt());
        assertEquals(1, invoiceRepository.findAll().stream()
                .filter(invoice -> fixture.invoiceId().equals(invoice.getOriginalInvoiceId()))
                .count());
    }

    private Object attemptRefund(
            UUID paymentId,
            CountDownLatch ready,
            CountDownLatch start
    ) throws InterruptedException {
        ready.countDown();
        start.await(5, TimeUnit.SECONDS);
        try {
            return service.refund(command(paymentId));
        } catch (RuntimeException exception) {
            return exception;
        }
    }

    private RefundFixture seedRefundablePayment() {
        UUID paymentId = UUID.randomUUID();
        UUID invoiceId = UUID.randomUUID();
        UUID visitId = UUID.randomUUID();
        paymentRepository.saveAndFlush(PaymentEntity.builder()
                .id(paymentId)
                .visitId(visitId)
                .examFee(new BigDecimal("100000"))
                .medicineFee(new BigDecimal("150000"))
                .totalAmount(new BigDecimal("250000"))
                .amountPaid(new BigDecimal("250000"))
                .paymentMethod(PaymentMethod.CASH)
                .status(PaymentStatus.SUCCESS)
                .collectedBy(UUID.randomUUID())
                .paidAt(PAID_AT)
                .createdAt(PAID_AT)
                .build());
        invoiceRepository.saveAndFlush(InvoiceEntity.builder()
                .id(invoiceId)
                .invoiceCode("HD-" + UUID.randomUUID().toString().substring(0, 8))
                .visitId(visitId)
                .paymentId(paymentId)
                .type(InvoiceType.ORIGINAL)
                .totalAmount(new BigDecimal("250000"))
                .createdBy(UUID.randomUUID())
                .createdAt(PAID_AT.plusSeconds(60))
                .build());
        invoiceLineRepository.saveAllAndFlush(List.of(
                line(invoiceId, InvoiceLineType.EXAM_FEE, "Exam fee", "100000"),
                line(invoiceId, InvoiceLineType.MEDICINE_FEE, "Medicine fee", "150000")
        ));
        return new RefundFixture(paymentId, invoiceId);
    }

    private InvoiceLineEntity line(
            UUID invoiceId,
            InvoiceLineType type,
            String name,
            String amount
    ) {
        BigDecimal value = new BigDecimal(amount);
        return InvoiceLineEntity.builder()
                .id(UUID.randomUUID())
                .invoiceId(invoiceId)
                .lineType(type)
                .itemName(name)
                .quantity(1)
                .unitPrice(value)
                .amount(value)
                .createdAt(PAID_AT.plusSeconds(60))
                .build();
    }

    private RefundPaymentCommand command(UUID paymentId) {
        return new RefundPaymentCommand(paymentId, "Patient cancelled");
    }

    private record RefundFixture(UUID paymentId, UUID invoiceId) {
    }
}

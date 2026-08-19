package com.benhsoan.persistence.mapper.billing;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.benhsoan.domain.billing.Payment;
import com.benhsoan.domain.billing.enums.PaymentMethod;
import com.benhsoan.domain.billing.enums.PaymentStatus;

class PaymentPersistenceMapperTest {

    private final PaymentPersistenceMapper mapper = new PaymentPersistenceMapper();

    @Test
    void roundTripsRefundMetadata() {
        UUID refundedBy = UUID.randomUUID();
        Instant refundedAt = Instant.parse("2026-08-18T04:30:00Z");
        Payment payment = Payment.restore(
                UUID.randomUUID(),
                UUID.randomUUID(),
                new BigDecimal("100000"),
                new BigDecimal("50000"),
                new BigDecimal("75000"),
                new BigDecimal("225000"),
                new BigDecimal("225000"),
                PaymentMethod.CASH,
                PaymentStatus.REFUNDED,
                UUID.randomUUID(),
                Instant.parse("2026-08-17T03:00:00Z"),
                "Patient cancelled the remaining services",
                refundedBy,
                refundedAt,
                Instant.parse("2026-08-17T03:00:00Z")
        );

        Payment restored = mapper.toDomain(mapper.toEntity(payment));

        assertEquals("Patient cancelled the remaining services", restored.getRefundReason());
        assertEquals(refundedBy, restored.getRefundedBy());
        assertEquals(refundedAt, restored.getRefundedAt());
        assertEquals(new BigDecimal("75000"), restored.getServiceFee());
    }
}

package com.benhsoan.port.outbound.repository.billing;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

import com.benhsoan.domain.billing.Payment;
import com.benhsoan.domain.billing.enums.PaymentStatus;

public interface PaymentRepository {

    Payment save(Payment payment);

    Optional<Payment> findById(UUID id);

    Optional<Payment> findByIdForUpdate(UUID id);

    Optional<Payment> findByVisitId(UUID visitId);

    BigDecimal sumAmountPaidByStatusInAndPaidAtBetween(
            Collection<PaymentStatus> statuses,
            Instant fromInclusive,
            Instant toExclusive
    );

    BigDecimal sumRefundedAmountByRefundedAtBetween(
            Instant fromInclusive,
            Instant toExclusive
    );
}

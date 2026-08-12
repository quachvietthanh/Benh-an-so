package com.benhsoan.port.outbound.repository.billing;

import java.util.Optional;
import java.util.UUID;

import com.benhsoan.domain.billing.Payment;

public interface PaymentRepository {

    Payment save(Payment payment);

    Optional<Payment> findById(UUID id);

    Optional<Payment> findByVisitId(UUID visitId);
}

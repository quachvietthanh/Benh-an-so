package com.benhsoan.port.outbound.repository.billing;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import com.benhsoan.domain.billing.PaymentServiceFee;

public interface PaymentServiceFeeRepository {

    List<PaymentServiceFee> saveAll(Collection<PaymentServiceFee> fees);

    List<PaymentServiceFee> findAllByPaymentId(UUID paymentId);
}

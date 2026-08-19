package com.benhsoan.application.ucservice.billing;

import org.springframework.stereotype.Component;

import com.benhsoan.domain.billing.Payment;
import com.benhsoan.port.dto.result.PaymentResult;

@Component
public class PaymentResultMapper {

    public PaymentResult toResult(Payment payment) {
        return new PaymentResult(
                payment.getId(),
                payment.getVisitId(),
                payment.getExamFee(),
                payment.getMedicineFee(),
                payment.getServiceFee(),
                payment.getTotalAmount(),
                payment.getAmountPaid(),
                payment.getPaymentMethod(),
                payment.getStatus(),
                payment.getCollectedBy(),
                payment.getPaidAt(),
                payment.getCreatedAt()
        );
    }
}

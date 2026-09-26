package com.benhsoan.application.ucservice.billing;

import java.util.List;

import org.springframework.stereotype.Component;

import com.benhsoan.domain.billing.Payment;
import com.benhsoan.port.dto.result.PaymentMethodItemResult;
import com.benhsoan.port.dto.result.PaymentResult;

@Component
public class PaymentResultMapper {

    public PaymentResult toResult(Payment payment) {
        List<PaymentMethodItemResult> methodResults = List.of();
        if (payment.getPaymentMethodItems() != null) {
            methodResults = payment.getPaymentMethodItems().stream()
                    .map(item -> new PaymentMethodItemResult(
                            item.getId(),
                            item.getPaymentId(),
                            item.getPaymentMethod(),
                            item.getAmount(),
                            item.getReferenceNumber(),
                            item.getCreatedAt()
                    ))
                    .toList();
        }

        return new PaymentResult(
                payment.getId(),
                payment.getVisitId(),
                payment.getExamFee(),
                payment.getMedicineFee(),
                payment.getServiceFee(),
                payment.getDiscountAmount(),
                payment.getDiscountRequestId(),
                payment.getTotalAmount(),
                payment.getAmountPaid(),
                payment.getPaymentMethod(),
                payment.getStatus(),
                payment.getCollectedBy(),
                payment.getPaidAt(),
                payment.getCreatedAt(),
                methodResults
        );
    }
}
